package com.iot.riff.service.request;

import com.iot.riff.service.domain.IotDeviceId;

public record IotDeviceDeleteRequest(String requestId, IotDeviceId id) implements BaseRequest {
}
