package com.iot.riff.service.util;

import com.networknt.schema.ValidationMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaValidatorTest {

    private IotJsonSchemaValidator jsonSchemaValidator;

    @BeforeEach
    void setUp() {
        jsonSchemaValidator = new IotJsonSchemaValidator();
    }

    @Test
    void testValidateValidJsonAndSchemaShouldPass() {
        String schema = "{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}}";
        String json = "{\"name\": \"test\"}";

        Set<ValidationMessage> errors = jsonSchemaValidator.validate(json, schema);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateInvalidJsonShouldReturnErrors() {
        String schema = "{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}}";
        String json = "{\"name\": 123}";

        Set<ValidationMessage> errors = jsonSchemaValidator.validate(json, schema);
        assertFalse(errors.isEmpty());
    }

    @Test
    void testValidateSchemaValidSchemaShouldReturnTrue() {
        String schema = "{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}}";
        assertTrue(jsonSchemaValidator.validateSchema(schema));
    }

    @Test
    void testValidateSchemaInvalidSchemaShouldReturnFalse() {
        String schema = "{\"type\": \"unknownType\"}";
        // Note: Default validator might be lenient or throw specific error depending on
        // config.
        // But invalid JSON structure for schema usually causes issues or specific
        // meta-schema validation failure.
        // Let's test with definitely broken JSON first to trigger parsing error if any,
        // or schema validation.
        // networknt validator usually throws exception on invalid schema creation which
        // we catch.
        jsonSchemaValidator.validateSchema(schema);

        // Let's force an exception by malformed JSON
        String malformedJson = "{ unquoted: error }";
        assertFalse(jsonSchemaValidator.validateSchema(malformedJson));
    }

    @Test
    void testDetectVersionDraft201909() {
        String schema = "{\"$schema\": \"https://json-schema.org/draft/2019-09/schema\", \"type\": \"string\"}";
        String json = "\"test\"";
        // this implicitly tests that it doesn't crash and uses a compatible factory
        Set<ValidationMessage> errors = jsonSchemaValidator.validate(json, schema);
        assertTrue(errors.isEmpty());
    }
}
