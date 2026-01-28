package com.iot.riff.service.request;

public record IotDeviceListRequest(String requestId, String name, String iotDeviceModelId, int limit, int page,
                String sort, String sortBy)
                implements BaseRequest {
}
