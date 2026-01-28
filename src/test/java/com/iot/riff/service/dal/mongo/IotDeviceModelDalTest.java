package com.iot.riff.service.dal.mongo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iot.riff.service.domain.IotDeviceModel;
import com.iot.riff.service.domain.IotDeviceModelId;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IotDeviceModelDalTest {

    @Mock
    private MongoClient mongoClient;

    @Mock
    private MongoDatabase mongoDatabase;

    @Mock
    private MongoCollection<Document> mongoCollection;

    // Removed @InjectMocks
    private IotDeviceModelDal iotDeviceModelDal;

    @BeforeEach
    void setUp() {
        iotDeviceModelDal = new IotDeviceModelDal("iot");
        iotDeviceModelDal.mongoClient = mongoClient; // Manually injecting protected field from BaseMongoOperation

        when(mongoClient.getDatabase("iot")).thenReturn(mongoDatabase);
        when(mongoDatabase.getCollection("device_model")).thenReturn(mongoCollection);
    }

    @Test
    void testSave() {
        // Given
        IotDeviceModelId id = new IotDeviceModelId("test-id");
        Document telemetrySchema = new Document("temp", "double");
        Document metadataSchema = new Document("location", "string");
        IotDeviceModel model = new IotDeviceModel(id, "Test Model", "Description", telemetrySchema, metadataSchema,
                java.time.Instant.now());

        // When
        iotDeviceModelDal.save(model);

        // Then
        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(mongoCollection).insertOne(docCaptor.capture());

        Document capturedDoc = docCaptor.getValue();
        assertEquals("test-id", capturedDoc.getString("_id"));
        assertEquals("Test Model", capturedDoc.getString("name"));
        assertEquals("Description", capturedDoc.getString("description"));
        assertEquals(telemetrySchema, capturedDoc.get("telemetry_schema"));
        assertEquals(metadataSchema, capturedDoc.get("metadata_schema"));
    }

    @Test
    void testGet() {
        // Given
        String id = "test-id";
        Document telemetrySchema = new Document("temp", "double");
        Document metadataSchema = new Document("location", "string");

        Document doc = new Document("_id", id)
                .append("name", "Test Model")
                .append("description", "Description")
                .append("telemetry_schema", telemetrySchema)
                .append("metadata_schema", metadataSchema);

        FindIterable<Document> findIterable = mock(FindIterable.class);
        when(mongoCollection.find(any(org.bson.conversions.Bson.class))).thenReturn(findIterable);
        when(findIterable.first()).thenReturn(doc);

        // When
        IotDeviceModel result = iotDeviceModelDal.get(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.id().id());
        assertEquals("Test Model", result.name());
        assertEquals("Description", result.description());
        assertEquals(telemetrySchema, result.telemetrySchema());
        assertEquals(metadataSchema, result.metadataSchema());
    }
}
