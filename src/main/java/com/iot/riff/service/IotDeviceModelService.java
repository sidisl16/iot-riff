package com.iot.riff.service;

import com.iot.riff.service.request.IotDeviceModelCreateRequest;
import com.iot.riff.service.request.IotDeviceModelDeleteRequest;
import com.iot.riff.service.request.IotDeviceModelGetRequest;
import com.iot.riff.service.request.IotDeviceModelListRequest;
import com.iot.riff.service.response.IotDeviceModelCreateResponse;
import com.iot.riff.service.response.IotDeviceModelGetResponse;
import com.iot.riff.service.response.IotDeviceModelListResponse;

public interface IotDeviceModelService {

    public IotDeviceModelCreateResponse create(IotDeviceModelCreateRequest request);

    public IotDeviceModelGetResponse get(IotDeviceModelGetRequest request);

    public IotDeviceModelListResponse list(IotDeviceModelListRequest request);

    public void delete(IotDeviceModelDeleteRequest request);
}