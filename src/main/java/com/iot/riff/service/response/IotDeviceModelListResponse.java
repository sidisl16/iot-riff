package com.iot.riff.service.response;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import com.iot.riff.service.domain.IotDeviceModel;

@Serdeable
public record IotDeviceModelListResponse(String requestId, List<IotDeviceModel> iotDeviceModels)
        implements BaseResponse {

}
