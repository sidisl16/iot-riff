package com.iot.riff.service.request;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record IotDeviceModelCreateRequest(String requestId, String name, String description,
        @NotNull @NotEmpty Map<String, Object> metadataSchema, @NotNull @NotEmpty Map<String, Object> telemetrySchema)
        implements BaseRequest {
}
