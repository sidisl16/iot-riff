package com.iot.riff.service.domain;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record MqttConnectionDetails(String host, int port, String topic, String username, String secretPath) {
}
