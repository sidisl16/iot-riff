package com.iot.riff.service.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.riff.service.exception.IotException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
@Singleton
public class IotJsonSchemaValidator {

    private final ObjectMapper objectMapper;

    public IotJsonSchemaValidator() {
        this.objectMapper = new ObjectMapper();
    }

    public Set<ValidationMessage> validate(String json, String schema) {
        SpecVersion.VersionFlag versionFlag = detectVersion(schema);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(versionFlag);
        JsonSchema jsonSchema = factory.getSchema(schema);
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            return jsonSchema.validate(jsonNode);
        } catch (Exception e) {
            throw new IotException("Failed to validate JSON against schema", e);
        }
    }

    public boolean validateSchema(String schema) {
        SpecVersion.VersionFlag versionFlag = detectVersion(schema);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(versionFlag);
        try {
            factory.getSchema(schema);
            return true;
        } catch (Exception e) {
            log.warn("Invalid schema", e);
        }
        return false;
    }

    public boolean validateSchema(java.util.Map<String, Object> schemaMap) {
        try {
            String schemaString = objectMapper.writeValueAsString(schemaMap);
            return validateSchema(schemaString);
        } catch (Exception e) {
            log.warn("Failed to serialize schema map", e);
            return false;
        }
    }

    private SpecVersion.VersionFlag detectVersion(String schema) {
        try {
            JsonNode schemaNode = objectMapper.readTree(schema);
            JsonNode metaSchema = schemaNode.get("$schema");
            if (metaSchema != null && metaSchema.isTextual()) {
                String schemaUri = metaSchema.asText();
                if (schemaUri.contains("json-schema.org/draft/2020-12/schema")) {
                    return SpecVersion.VersionFlag.V202012;
                } else if (schemaUri.contains("json-schema.org/draft/2019-09/schema")) {
                    return SpecVersion.VersionFlag.V201909;
                } else if (schemaUri.contains("json-schema.org/draft-07/schema")) {
                    return SpecVersion.VersionFlag.V7;
                } else if (schemaUri.contains("json-schema.org/draft-06/schema")) {
                    return SpecVersion.VersionFlag.V6;
                } else if (schemaUri.contains("json-schema.org/draft-04/schema")) {
                    return SpecVersion.VersionFlag.V4;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse schema for version detection, defaulting to V7", e);
        }
        return SpecVersion.VersionFlag.V7;
    }
}
