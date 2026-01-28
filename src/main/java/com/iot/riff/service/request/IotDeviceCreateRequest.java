package com.iot.riff.service.request;

import java.util.Map;

public record IotDeviceCreateRequest(String requestId, String name, String description, String iotDeviceModelId,
        Map<String, Object> metadata) implements BaseRequest {
}
