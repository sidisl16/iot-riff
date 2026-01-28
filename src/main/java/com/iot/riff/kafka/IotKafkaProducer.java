package com.iot.riff.kafka;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;

@KafkaClient
public interface IotKafkaProducer {

    @Topic("iot-device-data")
    void send(@KafkaKey String deviceId, String message);
}
