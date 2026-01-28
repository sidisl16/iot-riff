package com.iot.riff.service.domain;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.Map;

@Serdeable
public record IotDeviceModel(IotDeviceModelId id, String name, String description, Map<String, Object> telemetrySchema,
        Map<String, Object> metadataSchema, Instant createdAt) {
}
