package com.iot.riff.service.request;

public record IotDeviceModelListRequest(String requestId, String name, int limit, int page, String sort, String sortBy)
        implements BaseRequest {
}
