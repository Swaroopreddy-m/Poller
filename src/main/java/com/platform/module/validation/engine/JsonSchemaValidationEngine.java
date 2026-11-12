package com.platform.module.validation.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.platform.common.exception.PlatformException;
import com.platform.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * High-performance Dynamic JSON Schema Validation Engine.
 * Precompiles and caches versioned schemas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonSchemaValidationEngine {

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    private final ConcurrentHashMap<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();

    public void validatePayload(String schemaKey, String rawJsonSchema, Object payloadObj) {
        try {
            JsonSchema schema = schemaCache.computeIfAbsent(schemaKey, k -> schemaFactory.getSchema(rawJsonSchema));
            JsonNode targetNode = objectMapper.valueToTree(payloadObj);

            Set<ValidationMessage> validationMessages = schema.validate(targetNode);
            if (!validationMessages.isEmpty()) {
                List<ApiResponse.ValidationErrorItem> errors = validationMessages.stream()
                        .map(msg -> ApiResponse.ValidationErrorItem.builder()
                                .field(msg.getInstanceLocation().toString())
                                .code("SCHEMA_VALIDATION_FAILED")
                                .message(msg.getMessage())
                                .build())
                        .collect(Collectors.toList());

                throw new PlatformException("Request payload violates dynamic JSON schema requirements",
                        HttpStatus.BAD_REQUEST, "SCHEMA_VALIDATION_FAILED");
            }
        } catch (PlatformException pe) {
            throw pe;
        } catch (Exception e) {
            log.error("Failed executing schema validation for schemaKey: {}", schemaKey, e);
            throw new PlatformException("Malformed request or validation error", HttpStatus.BAD_REQUEST, "VALIDATION_ENGINE_ERROR");
        }
    }
}
