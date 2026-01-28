package com.iot.riff.service.request;

import com.iot.riff.service.domain.IotDeviceId;

public record IotDeviceGetRequest(String requestId, IotDeviceId iotDeviceId) implements BaseRequest {
}
