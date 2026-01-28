package com.iot.riff.kafka;

import com.iot.riff.service.IotDeviceDataService;
import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IotKafkaConsumerTest {

    @Mock
    private IotDeviceDataService iotDeviceDataService;
    @Mock
    private ObjectMapper objectMapper;

    private IotKafkaConsumer iotKafkaConsumer;

    @BeforeEach
    void setUp() {
        iotKafkaConsumer = new IotKafkaConsumer(iotDeviceDataService, objectMapper);
    }

    @Test
    void receive_validMessage_callsService() throws Exception {
        String deviceId = "device123";
        String message = "{\"payload\": {\"temp\": 25}}";
        Map<String, Object> payloadMap = Map.of("temp", 25);

        // Mock ObjectMapper behavior
        when(objectMapper.readValue(eq(message), any(io.micronaut.core.type.Argument.class)))
                .thenReturn(Map.of("payload", payloadMap));

        iotKafkaConsumer.receive(deviceId, message);

        verify(iotDeviceDataService).processTelemetry(deviceId, payloadMap);
    }
}
