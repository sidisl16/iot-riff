package com.iot.riff.mqtt;

import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class IotMqttMessageProcessor {

    private final com.iot.riff.kafka.IotKafkaProducer kafkaProducer;
    private final io.micronaut.serde.ObjectMapper objectMapper;

    public IotMqttMessageProcessor(com.iot.riff.kafka.IotKafkaProducer kafkaProducer,
            io.micronaut.serde.ObjectMapper objectMapper) {
        this.kafkaProducer = kafkaProducer;
        this.objectMapper = objectMapper;
    }

    public void process(String deviceId, String topic, String payload) {
        log.info("Processing message - DeviceId: {}, Topic: {}, Payload: {}", deviceId, topic, payload);
        try {
            var message = java.util.Map.of(
                    "deviceId", deviceId,
                    "topic", topic,
                    "payload", payload);
            String jsonMessage = objectMapper.writeValueAsString(message);
            kafkaProducer.send(deviceId, jsonMessage);
        } catch (Exception e) {
            log.error("Failed to publish message to Kafka", e);
        }
    }
}
