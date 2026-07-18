package io.osb.domain.templates;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Post-processes rendered JSON template trees so boolean placeholders can stay
 * quoted in the editable template (valid JSON) and still become real booleans
 * for HTTP/K8s APIs (e.g. Keycloak {@code credentials.temporary}).
 */
public final class JsonTemplateValues {

    private static final Set<String> BOOLEAN_KEYS = Set.of(
            "temporary",
            "enabled",
            "emailVerified",
            "verifyEmail",
            "loginWithEmailAllowed",
            "registrationAllowed",
            "publicClient",
            "serviceAccountsEnabled",
            "standardFlowEnabled",
            "directAccessGrantsEnabled");

    private JsonTemplateValues() {}

    @SuppressWarnings("unchecked")
    public static Object coerceBooleans(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                String key = String.valueOf(entry.getKey());
                Object child = entry.getValue();
                if (BOOLEAN_KEYS.contains(key) && child instanceof String text) {
                    Boolean parsed = parseBoolean(text);
                    out.put(key, parsed != null ? parsed : child);
                } else {
                    out.put(key, coerceBooleans(child));
                }
            }
            return out;
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(coerceBooleans(item));
            }
            return out;
        }
        return value;
    }

    private static Boolean parseBoolean(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equals(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }
}
