package com.iot.riff.kafka;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.core.type.Argument;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@KafkaListener(groupId = "iot-data-processor")
public class IotKafkaConsumer {

    private final com.iot.riff.service.IotDeviceDataService iotDeviceDataService;
    private final io.micronaut.serde.ObjectMapper objectMapper;

    public IotKafkaConsumer(com.iot.riff.service.IotDeviceDataService iotDeviceDataService,
            io.micronaut.serde.ObjectMapper objectMapper) {
        this.iotDeviceDataService = iotDeviceDataService;
        this.objectMapper = objectMapper;
    }

    @Topic("iot-device-data")
    public void receive(@KafkaKey String deviceId, String message) {
        log.info("Consumed Kafka message: key={}, value={}", deviceId, message);
        try {
            // Message structure from processor: {deviceId, topic, payload}
            java.util.Map<String, Object> messageMap = objectMapper.readValue(message,
                    Argument.mapOf(String.class, Object.class));

            // "payload" is likely a string JSON from the device or just a string.
            // The domain "telemetryPayload" implies a structured Map.
            // Let's try to parse "payload" as a Map if it's a string, or put it as is.
            Object payloadObj = messageMap.get("payload");
            java.util.Map<String, Object> telemetryPayload;

            if (payloadObj instanceof String) {
                try {
                    telemetryPayload = objectMapper.readValue((String) payloadObj,
                            Argument.mapOf(String.class, Object.class));
                } catch (Exception e) {
                    // Not a JSON object, maybe raw value
                    telemetryPayload = java.util.Map.of("raw", payloadObj);
                }
            } else if (payloadObj instanceof java.util.Map) {
                telemetryPayload = (java.util.Map<String, Object>) payloadObj;
            } else {
                telemetryPayload = java.util.Map.of("value", payloadObj);
            }

            iotDeviceDataService.processTelemetry(deviceId, telemetryPayload);

        } catch (Exception e) {
            log.error("Error processing message", e);
        }
    }
}
