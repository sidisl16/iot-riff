package com.iot.riff.service;

import java.util.Map;

public interface IotDeviceDataService {
    void processTelemetry(String deviceId, Map<String, Object> telemetryPayload);

    java.util.List<com.iot.riff.service.domain.DeviceData> searchData(String deviceId, java.time.Instant start,
            java.time.Instant end, int limit, int page);
}
