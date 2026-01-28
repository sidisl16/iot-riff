package com.iot.riff.service.dal.mongo;

import com.iot.riff.service.domain.IotDeviceModel;
import com.iot.riff.service.domain.IotDeviceModelId;

import org.bson.Document;

import jakarta.inject.Singleton;

import jakarta.annotation.PostConstruct;
import com.mongodb.client.model.Indexes;
import io.micronaut.context.annotation.Property;

@Singleton
public class IotDeviceModelDal extends BaseMongoOperation<IotDeviceModel> {

    private final String databaseName;
    private static final String COLLECTION_NAME = "device_model";
    private static final String FIELD_ID = "_id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_TELEMETRY_SCHEMA = "telemetry_schema";
    private static final String FIELD_METADATA_SCHEMA = "metadata_schema";
    private static final String FIELD_CREATED_AT = "created_at";

    public IotDeviceModelDal(@Property(name = "mongodb.database") String databaseName) {
        this.databaseName = databaseName;
    }

    @PostConstruct
    public void init() {
        getCollection().createIndex(Indexes.ascending(FIELD_NAME));
    }

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    protected String getCollectionName() {
        return COLLECTION_NAME;
    }

    @Override
    protected Mapper<IotDeviceModel> getMapper() {
        return new Mapper<>(this::toModel, this::toDocument);
    }

    private IotDeviceModel toModel(Document doc) {
        java.util.Date createdAtDate = doc.getDate(FIELD_CREATED_AT);
        java.time.Instant createdAt = createdAtDate != null ? createdAtDate.toInstant() : null;

        return new IotDeviceModel(
                new IotDeviceModelId(doc.get(FIELD_ID).toString()),
                doc.getString(FIELD_NAME),
                doc.getString(FIELD_DESCRIPTION),
                doc.get(FIELD_TELEMETRY_SCHEMA, java.util.Map.class),
                doc.get(FIELD_METADATA_SCHEMA, java.util.Map.class),
                createdAt);
    }

    private Document toDocument(IotDeviceModel model) {
        Document doc = new Document();
        if (model.id() != null) {
            doc.put(FIELD_ID, parseId(model.id().id()));
        }
        doc.put(FIELD_NAME, model.name());
        doc.put(FIELD_DESCRIPTION, model.description());
        doc.put(FIELD_TELEMETRY_SCHEMA, model.telemetrySchema());
        doc.put(FIELD_METADATA_SCHEMA, model.metadataSchema());
        if (model.createdAt() != null) {
            doc.put(FIELD_CREATED_AT, java.util.Date.from(model.createdAt()));
        }

        return doc;
    }

}
