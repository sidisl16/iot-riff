package com.iot.riff.service.domain;

import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;
import java.util.Map;

@Serdeable
public record DeviceData(
                String id,
                String deviceId,
                Map<String, Object> telemetryPayload,
                Instant receivedAt) {
}
