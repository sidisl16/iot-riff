package com.iot.riff.service.impl;

import org.bson.conversions.Bson;

import com.iot.riff.service.IotDeviceModelService;
import com.iot.riff.service.dal.mongo.BaseMongoOperation;
import com.iot.riff.service.dal.mongo.IotDeviceDal;
import com.iot.riff.service.domain.IotDeviceModel;
import com.iot.riff.service.exception.IotException;
import com.iot.riff.service.request.IotDeviceModelCreateRequest;
import com.iot.riff.service.request.IotDeviceModelDeleteRequest;
import com.iot.riff.service.request.IotDeviceModelGetRequest;
import com.iot.riff.service.request.IotDeviceModelListRequest;
import com.iot.riff.service.response.IotDeviceModelCreateResponse;
import com.iot.riff.service.response.IotDeviceModelGetResponse;
import com.iot.riff.service.response.IotDeviceModelListResponse;
import com.iot.riff.service.util.IotJsonSchemaValidator;
import com.mongodb.client.model.Filters;

import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
class IotDeviceModelServiceImpl implements IotDeviceModelService {

    private final BaseMongoOperation<IotDeviceModel> mongoOperation;
    private final IotJsonSchemaValidator jsonSchemaValidator;
    private final IotDeviceDal iotDeviceDal;

    public IotDeviceModelServiceImpl(BaseMongoOperation<IotDeviceModel> mongoOperation,
            IotJsonSchemaValidator jsonSchemaValidator,
            IotDeviceDal iotDeviceDal) {
        this.mongoOperation = mongoOperation;
        this.jsonSchemaValidator = jsonSchemaValidator;
        this.iotDeviceDal = iotDeviceDal;
    }

    @Override
    public IotDeviceModelCreateResponse create(IotDeviceModelCreateRequest request) {
        if (!jsonSchemaValidator.validateSchema(request.metadataSchema())) {
            throw new IotException("Invalid metadata schema");
        }
        if (!jsonSchemaValidator.validateSchema(request.telemetrySchema())) {
            throw new IotException("Invalid telemetry schema");
        }

        var savedModel = mongoOperation.save(new IotDeviceModel(null, request.name(), request.description(),
                request.telemetrySchema(), request.metadataSchema(), java.time.Instant.now()));

        return new IotDeviceModelCreateResponse(request.requestId(), savedModel);
    }

    @Override
    public IotDeviceModelGetResponse get(IotDeviceModelGetRequest request) {
        var model = mongoOperation.get(request.id().id());
        return new IotDeviceModelGetResponse(request.requestId(), model);
    }

    @Override
    public IotDeviceModelListResponse list(IotDeviceModelListRequest request) {
        Bson filter = Filters.exists("_id");
        if (request.name() != null) {
            filter = Filters.and(filter, Filters.eq("name", request.name()));
        }
        var models = mongoOperation.list(filter, request.limit(), request.page(), request.sort(), request.sortBy());
        return new IotDeviceModelListResponse(request.requestId(), models);
    }

    @Override
    public void delete(IotDeviceModelDeleteRequest request) {
        var model = mongoOperation.get(request.id().id());
        if (model == null) {
            throw new IotException("Device model not found");
        }
        if (iotDeviceDal.hasDeviceWithModelId(request.id().id())) {
            throw new IotException("Cannot delete device model as it is being used by one or more devices");
        }
        mongoOperation.delete(request.id().id());
    }
}
