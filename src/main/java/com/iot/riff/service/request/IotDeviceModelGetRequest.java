package com.iot.riff.service.request;

import com.iot.riff.service.domain.IotDeviceModelId;

public record IotDeviceModelGetRequest(String requestId, IotDeviceModelId id) implements BaseRequest {
}
