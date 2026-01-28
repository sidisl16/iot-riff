package com.iot.riff.service;

import com.iot.riff.service.request.IotDeviceCreateRequest;
import com.iot.riff.service.request.IotDeviceDeleteRequest;
import com.iot.riff.service.request.IotDeviceGetRequest;
import com.iot.riff.service.request.IotDeviceListRequest;
import com.iot.riff.service.response.IotDeviceCreateResponse;
import com.iot.riff.service.response.IotDeviceGetResponse;
import com.iot.riff.service.response.IotDeviceListResponse;

public interface IotDeviceService {

    IotDeviceCreateResponse create(IotDeviceCreateRequest request);

    IotDeviceGetResponse get(IotDeviceGetRequest request);

    IotDeviceListResponse list(IotDeviceListRequest request);

    void delete(IotDeviceDeleteRequest request);
}
