package com.iot.riff.service;

import com.iot.riff.service.dal.mongo.IotDeviceDal;
import com.iot.riff.service.dal.mongo.IotDeviceDataDal;
import com.iot.riff.service.dal.mongo.IotDeviceModelDal;
import com.iot.riff.service.domain.DeviceData;
import com.iot.riff.service.domain.IotDevice;
import com.iot.riff.service.domain.IotDeviceModel;
import com.iot.riff.service.domain.IotDeviceModelId;
import com.iot.riff.service.impl.IotDeviceDataServiceImpl;
import com.iot.riff.service.util.IotJsonSchemaValidator;
import com.networknt.schema.ValidationMessage;
import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IotDeviceDataServiceImplTest {

    @Mock
    private IotDeviceDataDal iotDeviceDataDal;
    @Mock
    private IotDeviceDal iotDeviceDal;
    @Mock
    private IotDeviceModelDal iotDeviceModelDal;
    @Mock
    private IotJsonSchemaValidator iotJsonSchemaValidator;
    @Mock
    private ObjectMapper objectMapper;

    private IotDeviceDataServiceImpl iotDeviceDataService;

    @BeforeEach
    void setUp() {
        iotDeviceDataService = new IotDeviceDataServiceImpl(iotDeviceDataDal, iotDeviceDal, iotDeviceModelDal,
                iotJsonSchemaValidator, objectMapper);
    }

    @Test
    void processTelemetry_validPayload_savesData() throws Exception {
        String deviceId = "device123";
        Map<String, Object> payloadMap = Map.of("temp", 25);
        Map<String, Object> schemaMap = Map.of("type", "object");
        String payloadJson = "{\"temp\": 25}";
        String schemaJson = "{\"type\": \"object\"}";

        IotDevice device = Mockito.mock(IotDevice.class);
        IotDeviceModel deviceModel = Mockito.mock(IotDeviceModel.class);
        IotDeviceModelId modelId = new IotDeviceModelId("model1");

        when(iotDeviceDal.get(deviceId)).thenReturn(device);
        when(device.iotDeviceModelId()).thenReturn(modelId);
        when(iotDeviceModelDal.get(modelId.id())).thenReturn(deviceModel);
        when(deviceModel.telemetrySchema()).thenReturn(schemaMap);
        when(objectMapper.writeValueAsString(payloadMap)).thenReturn(payloadJson);
        when(objectMapper.writeValueAsString(schemaMap)).thenReturn(schemaJson);
        when(iotJsonSchemaValidator.validate(payloadJson, schemaJson)).thenReturn(Collections.emptySet());

        iotDeviceDataService.processTelemetry(deviceId, payloadMap);

        verify(iotDeviceDataDal).save(any(DeviceData.class));
    }

    @Test
    void processTelemetry_invalidPayload_dropsMessage() throws Exception {
        String deviceId = "device123";
        Map<String, Object> payloadMap = Map.of("temp", "bad");
        Map<String, Object> schemaMap = Map.of("type", "object");
        String payloadJson = "{\"temp\": \"bad\"}";
        String schemaJson = "{\"type\": \"object\"}";

        IotDevice device = Mockito.mock(IotDevice.class);
        IotDeviceModel deviceModel = Mockito.mock(IotDeviceModel.class);
        IotDeviceModelId modelId = new IotDeviceModelId("model1");

        when(iotDeviceDal.get(deviceId)).thenReturn(device);
        when(device.iotDeviceModelId()).thenReturn(modelId);
        when(iotDeviceModelDal.get(modelId.id())).thenReturn(deviceModel);
        when(deviceModel.telemetrySchema()).thenReturn(schemaMap);
        when(objectMapper.writeValueAsString(payloadMap)).thenReturn(payloadJson);
        when(objectMapper.writeValueAsString(schemaMap)).thenReturn(schemaJson);

        ValidationMessage validationMessage = Mockito.mock(ValidationMessage.class);
        when(iotJsonSchemaValidator.validate(payloadJson, schemaJson)).thenReturn(Set.of(validationMessage));

        iotDeviceDataService.processTelemetry(deviceId, payloadMap);

        verify(iotDeviceDataDal, never()).save(any(DeviceData.class));
    }

    @Test
    void searchData_delegatesToDal() {
        String deviceId = "device123";
        java.time.Instant start = java.time.Instant.now().minusSeconds(3600);
        java.time.Instant end = java.time.Instant.now();
        int limit = 10;
        int page = 0;
        java.util.List<DeviceData> expected = Collections.emptyList();

        when(iotDeviceDataDal.findByDeviceIdAndTimeRange(deviceId, start, end, limit, page)).thenReturn(expected);

        java.util.List<DeviceData> actual = iotDeviceDataService.searchData(deviceId, start, end, limit, page);

        Assertions.assertEquals(expected, actual);
        verify(iotDeviceDataDal).findByDeviceIdAndTimeRange(deviceId, start, end, limit, page);
    }
}
