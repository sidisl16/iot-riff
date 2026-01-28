package com.iot.riff.service.response;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

import com.iot.riff.service.domain.IotDevice;

@Serdeable
public record IotDeviceListResponse(String requestId, List<IotDevice> iotDevices) implements BaseResponse {

}
