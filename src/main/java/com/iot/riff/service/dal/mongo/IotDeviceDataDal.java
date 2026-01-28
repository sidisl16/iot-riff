package com.iot.riff.service.dal.mongo;

import com.iot.riff.service.domain.DeviceData;
import com.mongodb.client.model.Indexes;
import io.micronaut.context.annotation.Property;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

import java.util.Map;

import org.bson.Document;

@Singleton
public class IotDeviceDataDal extends BaseMongoOperation<DeviceData> {

    private final String databaseName;
    private static final String COLLECTION_NAME = "device_data";

    private static final String FIELD_ID = "_id";
    private static final String FIELD_DEVICE_ID = "device_id";
    private static final String FIELD_TELEMETRY_PAYLOAD = "telemetry_payload";
    private static final String FIELD_RECEIVED_AT = "received_at";

    public IotDeviceDataDal(@Property(name = "mongodb.database") String databaseName) {
        this.databaseName = databaseName;
    }

    @PostConstruct
    public void init() {
        // Create index on received_at as requested
        getCollection().createIndex(Indexes.ascending(FIELD_RECEIVED_AT));
        // Also indexing device_id for efficient lookups by device
        getCollection().createIndex(Indexes.ascending(FIELD_DEVICE_ID));
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
    protected Mapper<DeviceData> getMapper() {
        return new Mapper<>(this::toModel, this::toDocument);
    }

    private DeviceData toModel(Document doc) {
        java.util.Date receivedAtDate = doc.getDate(FIELD_RECEIVED_AT);
        java.time.Instant receivedAt = receivedAtDate != null ? receivedAtDate.toInstant() : null;

        return new DeviceData(
                doc.getObjectId(FIELD_ID).toHexString(),
                doc.getString(FIELD_DEVICE_ID),
                doc.get(FIELD_TELEMETRY_PAYLOAD, Map.class),
                receivedAt);
    }

    private Document toDocument(DeviceData data) {
        Document doc = new Document();
        if (data.id() != null) {
            try {
                doc.put(FIELD_ID, new org.bson.types.ObjectId(data.id()));
            } catch (IllegalArgumentException e) {
                if (data.id() != null && !data.id().isEmpty()) {
                    doc.put(FIELD_ID, data.id());
                }
            }
        }

        doc.put(FIELD_DEVICE_ID, data.deviceId());
        doc.put(FIELD_TELEMETRY_PAYLOAD, data.telemetryPayload());
        doc.put(FIELD_RECEIVED_AT, java.util.Date.from(data.receivedAt()));
        return doc;
    }

    public java.util.List<DeviceData> findByDeviceIdAndTimeRange(String deviceId, java.time.Instant start,
            java.time.Instant end, int limit, int page) {
        org.bson.conversions.Bson filter = com.mongodb.client.model.Filters.and(
                com.mongodb.client.model.Filters.eq(FIELD_DEVICE_ID, deviceId),
                com.mongodb.client.model.Filters.gte(FIELD_RECEIVED_AT, start),
                com.mongodb.client.model.Filters.lte(FIELD_RECEIVED_AT, end));
        return list(filter, limit, page, "desc", FIELD_RECEIVED_AT);
    }
}
