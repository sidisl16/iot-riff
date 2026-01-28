package com.iot.riff.service.domain;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record IotDeviceId(String id) implements Id<String> {
}
