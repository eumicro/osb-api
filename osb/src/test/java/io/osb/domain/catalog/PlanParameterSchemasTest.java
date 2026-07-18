package io.osb.domain.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PlanParameterSchemasTest {

    private static final Map<String, Object> KEYCLOAK_SCHEMAS = Map.of(
            "service_instance",
            Map.of(
                    "create",
                    Map.of(
                            "parameters",
                            Map.of(
                                    "type",
                                    "object",
                                    "properties",
                                    Map.of(
                                            "displayName",
                                            Map.of("type", "string", "default", "OSB Realm"),
                                            "adminUsername",
                                            Map.of(
                                                    "type",
                                                    "string",
                                                    "minLength",
                                                    1,
                                                    "default",
                                                    "realm-admin"),
                                            "adminPassword",
                                            Map.of(
                                                    "type",
                                                    "string",
                                                    "minLength",
                                                    1,
                                                    "default",
                                                    "realm-admin"),
                                            "adminPasswordTemporary",
                                            Map.of("type", "boolean", "default", true)),
                                    "required",
                                    java.util.List.of("adminUsername", "adminPassword")))));

    @Test
    void appliesDefaultsWhenParametersEmpty() {
        Map<String, Object> normalized =
                PlanParameterSchemas.validateAndApplyDefaults(KEYCLOAK_SCHEMAS, Map.of());
        assertEquals("OSB Realm", normalized.get("displayName"));
        assertEquals("realm-admin", normalized.get("adminUsername"));
        assertEquals("realm-admin", normalized.get("adminPassword"));
        assertEquals(true, normalized.get("adminPasswordTemporary"));
    }

    @Test
    void keepsProvidedValues() {
        Map<String, Object> normalized = PlanParameterSchemas.validateAndApplyDefaults(
                KEYCLOAK_SCHEMAS,
                Map.of(
                        "displayName", "Acme",
                        "adminUsername", "alice",
                        "adminPassword", "s3cret"));
        assertEquals("Acme", normalized.get("displayName"));
        assertEquals("alice", normalized.get("adminUsername"));
        assertEquals("s3cret", normalized.get("adminPassword"));
    }

    @Test
    void rejectsBlankRequiredUsername() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> PlanParameterSchemas.validateAndApplyDefaults(
                        KEYCLOAK_SCHEMAS,
                        Map.of("adminUsername", "", "adminPassword", "x")));
        assertTrue(ex.getMessage().contains("invalid provision parameters"));
    }
}
