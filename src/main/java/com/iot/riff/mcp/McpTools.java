package com.iot.riff.mcp;

import com.iot.riff.service.IotDeviceDataService;
import com.iot.riff.service.IotDeviceModelService;
import com.iot.riff.service.IotDeviceService;
import com.iot.riff.service.request.*;
import com.iot.riff.service.response.*;
import com.iot.riff.service.domain.DeviceData;
import com.iot.riff.service.domain.IotDeviceId;
import com.iot.riff.service.domain.IotDeviceModelId;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Singleton
public class McpTools {

        @Inject
        IotDeviceService iotDeviceService;
        @Inject
        IotDeviceModelService iotDeviceModelService;
        @Inject
        IotDeviceDataService iotDeviceDataService;

        // Device Tools

        @Tool(description = "Creates a new IoT device in the system. Requires a name, a valid device model ID, and metadata that matches the model's metadata schema.")
        public IotDeviceCreateResponse createDevice(
                        @ToolArg(description = "A human-readable name for the device") String name,
                        @ToolArg(description = "A brief description of the device's purpose") String description,
                        @ToolArg(description = "The UUID of the model this device belongs to") String iotDeviceModelId,
                        @ToolArg(description = "Custom key-value pairs associated with the device, validated against the model metadata schema") Map<String, Object> metadata) {
                return iotDeviceService
                                .create(new IotDeviceCreateRequest(UUID.randomUUID().toString(), name, description,
                                                iotDeviceModelId, metadata));
        }

        @Tool(description = "Retrieves detailed information about a specific IoT device using its unique identifier.")
        public IotDeviceGetResponse getDevice(
                        @ToolArg(description = "The unique identifier of the device") String id) {
                return iotDeviceService.get(new IotDeviceGetRequest(UUID.randomUUID().toString(), new IotDeviceId(id)));
        }

        @Tool(description = "Lists IoT devices with optional filters. Supports pagination and sorting to manage large numbers of devices.")
        public IotDeviceListResponse listDevices(
                        @ToolArg(description = "Filter by device name (optional)") @Nullable String name,
                        @ToolArg(description = "Filter by device model identifier (optional)") @Nullable String iotDeviceModelId,
                        @ToolArg(description = "Maximum number of results to return") int limit,
                        @ToolArg(description = "The page index to retrieve (0-based)") int page,
                        @ToolArg(description = "Sort direction: 'asc' or 'desc'") String sort,
                        @ToolArg(description = "The field name to sort by (e.g., 'created_at')") String sortBy) {
                return iotDeviceService
                                .list(new IotDeviceListRequest(UUID.randomUUID().toString(), name, iotDeviceModelId,
                                                limit, page, sort, sortBy));
        }

        @Tool(description = "Permanently removes an IoT device from the system. Note: This action is irreversible and stops the device from connecting.")
        public String deleteDevice(
                        @ToolArg(description = "The unique identifier of the device to delete") String id) {
                iotDeviceService.delete(new IotDeviceDeleteRequest(UUID.randomUUID().toString(), new IotDeviceId(id)));
                return "Device " + id + " deleted successfully";
        }

        // Model Tools

        @Tool(description = "Defines a new IoT device model, which acts as a template for devices. Includes validation schemas for metadata and telemetry.")
        public IotDeviceModelCreateResponse createModel(
                        @ToolArg(description = "A human-readable name for the model") String name,
                        @ToolArg(description = "A brief description of the model") String description,
                        @ToolArg(description = "JSON Schema for device metadata") Map<String, Object> metadataSchema,
                        @ToolArg(description = "JSON Schema for telemetry payloads") Map<String, Object> telemetrySchema) {
                return iotDeviceModelService.create(new IotDeviceModelCreateRequest(UUID.randomUUID().toString(), name,
                                description, metadataSchema, telemetrySchema));
        }

        @Tool(description = "Retrieves the configuration, including metadata and telemetry schemas, for a specific device model.")
        public IotDeviceModelGetResponse getModel(
                        @ToolArg(description = "The unique identifier of the device model") String id) {
                return iotDeviceModelService
                                .get(new IotDeviceModelGetRequest(UUID.randomUUID().toString(),
                                                new IotDeviceModelId(id)));
        }

        @Tool(description = "Lists all available device models. Essential for discovering valid model IDs when creating new devices.")
        public IotDeviceModelListResponse listModels(
                        @ToolArg(description = "Filter by model name (optional)") @Nullable String name,
                        @ToolArg(description = "Maximum number of results to return") int limit,
                        @ToolArg(description = "The page index to retrieve (0-based)") int page,
                        @ToolArg(description = "Sort direction: 'asc' or 'desc'") String sort,
                        @ToolArg(description = "The field name to sort by") String sortBy) {
                return iotDeviceModelService
                                .list(new IotDeviceModelListRequest(UUID.randomUUID().toString(), name, limit, page,
                                                sort, sortBy));
        }

        @Tool(description = "Removes a device model definition. Failure occurs if any existing devices are still associated with this model.")
        public String deleteModel(
                        @ToolArg(description = "The unique UUID of the device model to delete") String id) {
                iotDeviceModelService
                                .delete(new IotDeviceModelDeleteRequest(UUID.randomUUID().toString(),
                                                new IotDeviceModelId(id)));
                return "Device model " + id + " deleted successfully";
        }

        // Data Tools

        @Tool(description = "Searches historical telemetry data for a specific device within a time range. Results are paginated.")
        public List<DeviceData> searchData(
                        @ToolArg(description = "The unique UUID of the device") String deviceId,
                        @ToolArg(description = "Start time in ISO-8601 format (e.g., 2023-01-01T00:00:00Z)") String start,
                        @ToolArg(description = "End time in ISO-8601 format (e.g., 2023-01-02T00:00:00Z)") String end,
                        @ToolArg(description = "Maximum number of results to return") int limit,
                        @ToolArg(description = "The page index to retrieve (0-based)") int page) {
                return iotDeviceDataService.searchData(deviceId, Instant.parse(start), Instant.parse(end), limit, page);
        }
}
