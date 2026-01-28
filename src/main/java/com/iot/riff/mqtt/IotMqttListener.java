package com.iot.riff.mqtt;

import jakarta.inject.Singleton;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import lombok.extern.slf4j.Slf4j;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import com.iot.riff.service.dal.mongo.IotDeviceDal;
import com.iot.riff.service.exception.IotException;

import io.micronaut.context.event.StartupEvent;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
@Slf4j
public class IotMqttListener implements io.micronaut.context.event.ApplicationEventListener<StartupEvent> {

    private final int port;
    private final IotDeviceDal iotDeviceDal;
    private final IotMqttMessageProcessor mqttMessageProcessor;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public IotMqttListener(IotMqttConfig config, IotDeviceDal iotDeviceDal,
            IotMqttMessageProcessor mqttMessageProcessor) {
        this.port = config.getPort();
        this.iotDeviceDal = iotDeviceDal;
        this.mqttMessageProcessor = mqttMessageProcessor;
    }

    @Override
    public void onApplicationEvent(StartupEvent event) {
        executor.submit(() -> {
            try {
                start();
            } catch (Exception e) {
                log.error("Failed to start MQTT listener", e);
                throw new IotException("Failed to start MQTT listener", e);
            }
        });
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping MQTT Listener...");
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        executor.shutdown();
    }

    public void start() throws Exception {
        bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // Add MQTT codec handlers
                            pipeline.addLast("decoder", new MqttDecoder());
                            pipeline.addLast("encoder", MqttEncoder.INSTANCE);

                            // Add custom MQTT message handler
                            pipeline.addLast("handler", new MqttMessageHandler(iotDeviceDal, mqttMessageProcessor));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();
            log.info("MQTT Listener started on port: {}", port);

            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.info("MQTT Listener interrupted");
        }
    }

    // Custom handler for MQTT messages
    private class MqttMessageHandler extends SimpleChannelInboundHandler<MqttMessage> {

        private final IotDeviceDal iotDeviceDal;
        private final IotMqttMessageProcessor mqttMessageProcessor;
        private String deviceId;

        public MqttMessageHandler(IotDeviceDal iotDeviceDal, IotMqttMessageProcessor mqttMessageProcessor) {
            this.iotDeviceDal = iotDeviceDal;
            this.mqttMessageProcessor = mqttMessageProcessor;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) {
            MqttFixedHeader fixedHeader = msg.fixedHeader();

            switch (fixedHeader.messageType()) {
                case CONNECT:
                    handleConnect(ctx, (MqttConnectMessage) msg);
                    break;

                case PUBLISH:
                    handlePublish(ctx, (MqttPublishMessage) msg);
                    break;

                case SUBSCRIBE:
                    handleSubscribe(ctx, (MqttSubscribeMessage) msg);
                    break;

                case UNSUBSCRIBE:
                    handleUnsubscribe(ctx, (MqttUnsubscribeMessage) msg);
                    break;

                case PINGREQ:
                    handlePingReq(ctx);
                    break;

                case DISCONNECT:
                    handleDisconnect(ctx);
                    break;

                default:
                    log.info("Received message type: {}", fixedHeader.messageType());
            }
        }

        private void handleConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
            MqttConnectPayload payload = msg.payload();
            String clientId = payload.clientIdentifier();
            log.info("Client connecting: {}", clientId);

            String username = payload.userName();
            byte[] passwordBytes = payload.passwordInBytes();
            String password = passwordBytes != null ? new String(passwordBytes, CharsetUtil.UTF_8) : null;

            if (username != null && password != null
                    && iotDeviceDal.findByCredentials(username, password).isPresent()) {
                this.deviceId = username;
                log.info("Client authenticated: {}, DeviceId: {}", clientId, deviceId);
                // Send CONNACK
                MqttFixedHeader connAckFixedHeader = new MqttFixedHeader(
                        MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
                MqttConnAckVariableHeader connAckVariableHeader = new MqttConnAckVariableHeader(
                        MqttConnectReturnCode.CONNECTION_ACCEPTED, false);
                MqttConnAckMessage connAck = new MqttConnAckMessage(connAckFixedHeader, connAckVariableHeader);
                ctx.writeAndFlush(connAck);
            } else {
                log.warn("Authentication failed for client: {}", clientId);
                // Send CONNACK with failure
                MqttFixedHeader connAckFixedHeader = new MqttFixedHeader(
                        MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
                MqttConnAckVariableHeader connAckVariableHeader = new MqttConnAckVariableHeader(
                        MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, false);
                MqttConnAckMessage connAck = new MqttConnAckMessage(connAckFixedHeader, connAckVariableHeader);
                ctx.writeAndFlush(connAck);
                ctx.close();
            }
        }

        private void handlePublish(ChannelHandlerContext ctx, MqttPublishMessage msg) {
            String topic = msg.variableHeader().topicName();
            String payload = msg.payload().toString(CharsetUtil.UTF_8);
            MqttQoS qos = msg.fixedHeader().qosLevel();

            log.info("Received PUBLISH:");
            log.info("  Topic: " + topic);
            log.info("  Payload: " + payload);
            log.info("  QoS: " + qos);
            log.info("  DeviceId: " + deviceId);

            if (deviceId != null) {
                mqttMessageProcessor.process(deviceId, topic, payload);
            } else {
                log.warn("Received PUBLISH from unauthenticated or unknown device");
            }

            // Send PUBACK for QoS 1
            if (qos == MqttQoS.AT_LEAST_ONCE) {
                int messageId = msg.variableHeader().packetId();
                MqttFixedHeader pubAckFixedHeader = new MqttFixedHeader(
                        MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
                MqttMessageIdVariableHeader pubAckVariableHeader = MqttMessageIdVariableHeader.from(messageId);
                MqttPubAckMessage pubAck = new MqttPubAckMessage(pubAckFixedHeader, pubAckVariableHeader);

                ctx.writeAndFlush(pubAck);
            }
        }

        private void handleSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage msg) {
            int messageId = msg.variableHeader().messageId();

            log.info("Received SUBSCRIBE (messageId: " + messageId + "):");
            for (MqttTopicSubscription subscription : msg.payload().topicSubscriptions()) {
                log.info("  Topic: " + subscription.topicName() +
                        ", QoS: " + subscription.qualityOfService());
            }

            // Send SUBACK
            MqttFixedHeader subAckFixedHeader = new MqttFixedHeader(
                    MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
            MqttMessageIdVariableHeader subAckVariableHeader = MqttMessageIdVariableHeader.from(messageId);
            MqttSubAckPayload subAckPayload = new MqttSubAckPayload(
                    msg.payload().topicSubscriptions().stream()
                            .mapToInt(s -> MqttQoS.FAILURE.value())
                            .toArray());
            MqttSubAckMessage subAck = new MqttSubAckMessage(
                    subAckFixedHeader, subAckVariableHeader, subAckPayload);

            ctx.writeAndFlush(subAck);
        }

        private void handleUnsubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage msg) {
            int messageId = msg.variableHeader().messageId();
            // Send UNSUBACK
            MqttFixedHeader unsubAckFixedHeader = new MqttFixedHeader(
                    MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
            MqttMessageIdVariableHeader unsubAckVariableHeader = MqttMessageIdVariableHeader.from(messageId);
            MqttUnsubAckMessage unsubAck = new MqttUnsubAckMessage(
                    unsubAckFixedHeader, unsubAckVariableHeader);

            ctx.writeAndFlush(unsubAck);
        }

        private void handlePingReq(ChannelHandlerContext ctx) {
            log.debug("Received PINGREQ");

            // Send PINGRESP
            MqttFixedHeader pingRespFixedHeader = new MqttFixedHeader(
                    MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
            MqttMessage pingResp = new MqttMessage(pingRespFixedHeader);

            ctx.writeAndFlush(pingResp);
        }

        private void handleDisconnect(ChannelHandlerContext ctx) {
            ctx.close();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}