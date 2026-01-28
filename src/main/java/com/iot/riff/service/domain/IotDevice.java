package com.iot.riff.service.domain;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.Map;

@Serdeable
public record IotDevice(IotDeviceId id, IotDeviceModelId iotDeviceModelId, String name, String description,
        MqttConnectionDetails mqttConnectionDetails, Map<String, Object> metadata, DeviceStatus status,
        Instant createdAt) {
}
