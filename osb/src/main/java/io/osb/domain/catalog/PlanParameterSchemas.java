package io.osb.domain.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.ApplyDefaultsStrategy;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads and validates OSB plan provision parameters using JSON Schema
 * ({@code schemas.service_instance.create.parameters}).
 */
public final class PlanParameterSchemas {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonSchemaFactory SCHEMA_FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    private PlanParameterSchemas() {}

    /**
     * Extracts the JSON Schema for service-instance create parameters from an OSB plan
     * {@code schemas} object. Returns an empty object schema when absent.
     */
    public static Map<String, Object> extractCreateParametersSchema(Map<String, Object> planSchemas) {
        JsonNode root = JSON.valueToTree(planSchemas == null ? Map.of() : planSchemas);
        JsonNode parameters = root.path("service_instance").path("create").path("parameters");
        if (parameters.isMissingNode() || parameters.isNull() || !parameters.isObject()) {
            return Map.of("type", "object", "properties", Map.of());
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = JSON.convertValue(parameters, Map.class);
        return Map.copyOf(schema);
    }

    /**
     * Applies JSON Schema defaults and validates {@code parameters} against the plan create schema.
     *
     * @return normalized parameters (defaults filled in)
     * @throws IllegalArgumentException when validation fails
     */
    public static Map<String, Object> validateAndApplyDefaults(
            Map<String, Object> planSchemas, Map<String, Object> parameters) {
        Map<String, Object> schemaMap = extractCreateParametersSchema(planSchemas);
        JsonNode schemaNode = JSON.valueToTree(schemaMap);
        ObjectNode dataNode = parameters == null || parameters.isEmpty()
                ? JSON.createObjectNode()
                : JSON.valueToTree(parameters).deepCopy();

        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                .applyDefaultsStrategy(new ApplyDefaultsStrategy(true, true, true))
                .build();
        JsonSchema schema = SCHEMA_FACTORY.getSchema(schemaNode, config);
        Set<ValidationMessage> errors = schema.walk(dataNode, true).getValidationMessages();
        if (!errors.isEmpty()) {
            String detail = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("invalid provision parameters: " + detail);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> normalized = JSON.convertValue(dataNode, LinkedHashMap.class);
        return Map.copyOf(normalized == null ? Map.of() : normalized);
    }
}
