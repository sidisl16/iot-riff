package com.iot.riff.service.impl;

import com.iot.riff.mqtt.IotMqttConfig;
import com.iot.riff.service.IotDeviceService;
import com.iot.riff.service.dal.mongo.IotDeviceDal;
import com.iot.riff.service.dal.mongo.IotDeviceModelDal;
import com.iot.riff.service.request.IotDeviceCreateRequest;
import com.iot.riff.service.request.IotDeviceDeleteRequest;
import com.iot.riff.service.request.IotDeviceGetRequest;
import com.iot.riff.service.request.IotDeviceListRequest;
import com.iot.riff.service.response.IotDeviceCreateResponse;
import com.iot.riff.service.response.IotDeviceGetResponse;
import com.iot.riff.service.response.IotDeviceListResponse;
import com.iot.riff.service.util.IotJsonSchemaValidator;
import com.mongodb.client.model.Filters;
import io.micronaut.json.JsonMapper;
import com.iot.riff.service.domain.IotDeviceModel;
import com.iot.riff.service.exception.IotException;
import com.networknt.schema.ValidationMessage;
import com.iot.riff.service.domain.DeviceStatus;
import com.iot.riff.service.domain.IotDevice;
import com.iot.riff.service.domain.IotDeviceModelId;
import com.iot.riff.service.domain.MqttConnectionDetails;

import com.iot.riff.vault.IotVaultService;
import java.util.Set;

import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class IotDeviceServiceImpl implements IotDeviceService {

    private final IotDeviceDal iotDeviceDal;
    private final IotDeviceModelDal iotDeviceModelDal;
    private final IotJsonSchemaValidator jsonSchemaValidator;
    private final JsonMapper objectMapper;
    private final IotMqttConfig iotMqttConfig;
    private final IotVaultService iotVaultService;

    public IotDeviceServiceImpl(IotDeviceDal iotDeviceDal, IotDeviceModelDal iotDeviceModelDal,
            IotJsonSchemaValidator jsonSchemaValidator, JsonMapper objectMapper,
            IotMqttConfig iotMqttConfig, IotVaultService iotVaultService) {
        this.iotDeviceDal = iotDeviceDal;
        this.iotDeviceModelDal = iotDeviceModelDal;
        this.jsonSchemaValidator = jsonSchemaValidator;
        this.objectMapper = objectMapper;
        this.iotMqttConfig = iotMqttConfig;
        this.iotVaultService = iotVaultService;
    }

    @Override
    public IotDeviceCreateResponse create(IotDeviceCreateRequest request) {
        if (request.name() == null || request.name().isEmpty()) {
            throw new IotException("Name is mandatory");
        }
        IotDeviceModel iotDeviceModel = iotDeviceModelDal.get(request.iotDeviceModelId());
        if (iotDeviceModel == null) {
            throw new IotException("IotDeviceModel not found for id: " + request.iotDeviceModelId());
        }

        if (iotDeviceModel.metadataSchema() != null) {
            try {
                String metadataJson = objectMapper.writeValueAsString(request.metadata());
                String schemaJson = objectMapper.writeValueAsString(iotDeviceModel.metadataSchema());
                Set<ValidationMessage> validationMessages = jsonSchemaValidator.validate(metadataJson, schemaJson);
                if (!validationMessages.isEmpty()) {
                    throw new IotException("Metadata validation failed: " + validationMessages);
                }
            } catch (Exception e) {
                throw new IotException("Error validating metadata", e);
            }
        }

        var savedDevice = iotDeviceDal.save(new IotDevice(null,
                new IotDeviceModelId(request.iotDeviceModelId()),
                request.name(),
                request.description(),
                null,
                request.metadata(),
                DeviceStatus.ACTIVE,
                java.time.Instant.now()));

        try {
            String secretPath = iotVaultService.generateAndStoreSecretPath(savedDevice.id().id());
            var mqttConnectionDetails = getMqttConnectionDetails(savedDevice, secretPath);
            var updatedDevice = new IotDevice(savedDevice.id(),
                    savedDevice.iotDeviceModelId(),
                    savedDevice.name(),
                    savedDevice.description(),
                    mqttConnectionDetails,
                    savedDevice.metadata(),
                    savedDevice.status(),
                    savedDevice.createdAt());

            iotDeviceDal.update(updatedDevice);
            return new IotDeviceCreateResponse(request.requestId(), updatedDevice);
        } catch (Exception e) {
            log.error("Error securing device with Vault", e);
            // Rollback
            iotDeviceDal.delete(savedDevice.id().id());
            throw new IotException("Error during device creation: " + e.getMessage(), e);
        }
    }

    private MqttConnectionDetails getMqttConnectionDetails(IotDevice iotDevice, String secretPath) {
        return new MqttConnectionDetails(iotMqttConfig.getHost(),
                iotMqttConfig.getPort(),
                "iot/any",
                iotDevice.id().id(),
                secretPath);
    }

    @Override
    public IotDeviceGetResponse get(IotDeviceGetRequest request) {
        IotDevice device = iotDeviceDal.get(request.iotDeviceId().id());
        if (device == null) {
            throw new IotException("IotDevice not found for id: " + request.iotDeviceId().id());
        }
        return new IotDeviceGetResponse(request.requestId(), device);
    }

    @Override
    public IotDeviceListResponse list(IotDeviceListRequest request) {
        var filter = Filters.exists("_id");
        if (request.name() != null) {
            filter = Filters.and(filter, Filters.eq("name", request.name()));
        }
        if (request.iotDeviceModelId() != null) {
            filter = Filters.and(filter, Filters.eq("iotDeviceModelId", request.iotDeviceModelId()));
        }
        return new IotDeviceListResponse(request.requestId(),
                iotDeviceDal.list(filter, request.limit(), request.page(), request.sort(), request.sortBy()));
    }

    @Override
    public void delete(IotDeviceDeleteRequest request) {
        if (iotDeviceDal.get(request.id().id()) == null) {
            throw new IotException("IotDevice not found for id: " + request.id().id());
        }
        iotDeviceDal.delete(request.id().id());
    }

}
