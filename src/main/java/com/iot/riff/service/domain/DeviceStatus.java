package com.iot.riff.service.domain;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum DeviceStatus {
    ACTIVE, INACTIVE
}
