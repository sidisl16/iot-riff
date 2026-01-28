package com.iot.riff.service.response;

import io.micronaut.serde.annotation.Serdeable;
import com.iot.riff.service.domain.IotDeviceModel;

@Serdeable
public record IotDeviceModelCreateResponse(String requestId, IotDeviceModel iotDeviceModel) implements BaseResponse {
}
