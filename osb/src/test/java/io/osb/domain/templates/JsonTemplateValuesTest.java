package io.osb.domain.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTemplateValuesTest {

    @Test
    void coercesQuotedTemporaryFlag() {
        Object coerced = JsonTemplateValues.coerceBooleans(Map.of(
                "users",
                List.of(Map.of(
                        "username",
                        "test",
                        "credentials",
                        List.of(Map.of(
                                "type",
                                "password",
                                "value",
                                "secret",
                                "temporary",
                                "true"))))));

        @SuppressWarnings("unchecked")
        Map<String, Object> root = assertInstanceOf(Map.class, coerced);
        @SuppressWarnings("unchecked")
        List<Object> users = assertInstanceOf(List.class, root.get("users"));
        @SuppressWarnings("unchecked")
        Map<String, Object> user = assertInstanceOf(Map.class, users.get(0));
        @SuppressWarnings("unchecked")
        List<Object> credentials = assertInstanceOf(List.class, user.get("credentials"));
        @SuppressWarnings("unchecked")
        Map<String, Object> credential = assertInstanceOf(Map.class, credentials.get(0));
        assertEquals(Boolean.TRUE, credential.get("temporary"));
        assertEquals("secret", credential.get("value"));
    }
}
