package com.iot.riff;

import com.iot.riff.kafka.IotKafkaConsumer;
import com.iot.riff.mqtt.IotMqttListener;
import com.mongodb.client.MongoClient;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;
import org.mockito.Mockito;

@Factory
public class TestMocks {

    @Singleton
    @Replaces(MongoClient.class)
    public MongoClient mongoClient() {
        return Mockito.mock(MongoClient.class);
    }

    @Singleton
    @Replaces(IotKafkaConsumer.class)
    public IotKafkaConsumer iotKafkaConsumer() {
        return Mockito.mock(IotKafkaConsumer.class);
    }

    @Singleton
    @Replaces(IotMqttListener.class)
    public IotMqttListener iotMqttListener() {
        com.iot.riff.mqtt.IotMqttConfig config = new com.iot.riff.mqtt.IotMqttConfig("localhost", 1883, "password");
        return new IotMqttListener(config, null, null) {
            @Override
            public void onApplicationEvent(io.micronaut.context.event.StartupEvent event) {
                // Do nothing
            }

            @Override
            public void start() throws Exception {
                // Do nothing
            }

            @Override
            public void stop() {
                // Do nothing
            }
        };
    }
}
