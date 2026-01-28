package com.iot.riff.service.response;

import io.micronaut.serde.annotation.Serdeable;
import com.iot.riff.service.domain.IotDevice;

@Serdeable
public record IotDeviceGetResponse(String requestId, IotDevice iotDevice) implements BaseResponse {

}
