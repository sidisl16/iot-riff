package com.iot.riff.service.dal.mongo;

import com.iot.riff.service.domain.DeviceStatus;
import com.iot.riff.service.domain.IotDevice;
import com.iot.riff.service.domain.IotDeviceId;
import com.iot.riff.service.domain.IotDeviceModelId;
import com.iot.riff.service.domain.MqttConnectionDetails;
import com.mongodb.client.model.Indexes;
import io.micronaut.context.annotation.Property;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

import org.bson.Document;

@Singleton
public class IotDeviceDal extends BaseMongoOperation<IotDevice> {

    private final String databaseName;
    private static final String COLLECTION_NAME = "device";
    private static final String FIELD_ID = "_id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_IOT_DEVICE_MODEL_ID = "iot_device_model_id";
    private static final String FIELD_METADATA = "metadata";
    private static final String FIELD_MQTT = "mqtt_connection_details";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_CREATED_AT = "created_at";

    private static final String FIELD_MQTT_HOST = "host";
    private static final String FIELD_MQTT_PORT = "port";
    private static final String FIELD_MQTT_TOPIC = "topic";
    private static final String FIELD_MQTT_USERNAME = "username";
    private static final String FIELD_MQTT_SECRET_PATH = "secretPath";

    public IotDeviceDal(@Property(name = "mongodb.database") String databaseName) {
        this.databaseName = databaseName;
    }

    @PostConstruct
    public void init() {
        getCollection().createIndex(Indexes.ascending(FIELD_IOT_DEVICE_MODEL_ID));
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
    protected Mapper<IotDevice> getMapper() {
        return new Mapper<>(this::toModel, this::toDocument);
    }

    private IotDevice toModel(Document doc) {
        Document mqttDoc = doc.get(FIELD_MQTT, Document.class);
        MqttConnectionDetails mqtt = null;
        if (mqttDoc != null) {
            mqtt = new MqttConnectionDetails(
                    mqttDoc.getString(FIELD_MQTT_HOST),
                    mqttDoc.getInteger(FIELD_MQTT_PORT),
                    mqttDoc.getString(FIELD_MQTT_TOPIC),
                    mqttDoc.getString(FIELD_MQTT_USERNAME),
                    mqttDoc.getString(FIELD_MQTT_SECRET_PATH));
        }

        String statusStr = doc.getString(FIELD_STATUS);
        DeviceStatus status = statusStr != null ? DeviceStatus.valueOf(statusStr) : null;

        java.util.Date createdAtDate = doc.getDate(FIELD_CREATED_AT);
        java.time.Instant createdAt = createdAtDate != null ? createdAtDate.toInstant() : null;

        return new IotDevice(
                new IotDeviceId(doc.get(FIELD_ID).toString()),
                new IotDeviceModelId(doc.getString(FIELD_IOT_DEVICE_MODEL_ID)),
                doc.getString(FIELD_NAME),
                doc.getString(FIELD_DESCRIPTION),
                mqtt,
                doc.get(FIELD_METADATA, java.util.Map.class),
                status,
                createdAt);
    }

    public boolean hasDeviceWithModelId(String modelId) {
        return count(com.mongodb.client.model.Filters.eq(FIELD_IOT_DEVICE_MODEL_ID, modelId)) > 0;
    }

    public java.util.Optional<IotDevice> findByCredentials(String username, String secretPath) {
        org.bson.conversions.Bson filter = com.mongodb.client.model.Filters.and(
                com.mongodb.client.model.Filters.eq(FIELD_ID, parseId(username)),
                com.mongodb.client.model.Filters.eq(FIELD_MQTT + "." + FIELD_MQTT_SECRET_PATH, secretPath));
        Document doc = getCollection().find(filter).first();
        return doc != null ? java.util.Optional.of(getMapper().toModel().apply(doc)) : java.util.Optional.empty();
    }

    private Document toDocument(IotDevice device) {
        Document doc = new Document();
        if (device.id() != null) {
            doc.put(FIELD_ID, parseId(device.id().id()));
        }
        if (device.iotDeviceModelId() != null) {
            doc.put(FIELD_IOT_DEVICE_MODEL_ID, device.iotDeviceModelId().id());
        }
        doc.put(FIELD_NAME, device.name());
        doc.put(FIELD_DESCRIPTION, device.description());
        doc.put(FIELD_METADATA, device.metadata());
        if (device.status() != null) {
            doc.put(FIELD_STATUS, device.status().name());
        }
        if (device.createdAt() != null) {
            doc.put(FIELD_CREATED_AT, java.util.Date.from(device.createdAt()));
        }
        if (device.mqttConnectionDetails() != null) {
            Document mqttDoc = new Document();
            mqttDoc.put(FIELD_MQTT_HOST, device.mqttConnectionDetails().host());
            mqttDoc.put(FIELD_MQTT_PORT, device.mqttConnectionDetails().port());
            mqttDoc.put(FIELD_MQTT_TOPIC, device.mqttConnectionDetails().topic());
            mqttDoc.put(FIELD_MQTT_USERNAME, device.mqttConnectionDetails().username());
            mqttDoc.put(FIELD_MQTT_SECRET_PATH, device.mqttConnectionDetails().secretPath());
            doc.put(FIELD_MQTT, mqttDoc);
        }
        return doc;
    }
}
