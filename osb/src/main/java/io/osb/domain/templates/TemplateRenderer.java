package io.osb.domain.templates;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitutes {@code ${key}} and {@code ${parameters.x}} placeholders in template text.
 */
public final class TemplateRenderer {

    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\$\\{([a-zA-Z0-9_.-]+)}");

    private TemplateRenderer() {}

    public static String render(String content, Map<String, Object> values) {
        String text = Objects.requireNonNullElse(content, "");
        if (text.isEmpty() || values == null || values.isEmpty()) {
            return text;
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = resolve(values, key);
            matcher.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static Object resolve(Map<String, Object> values, String key) {
        if (values.containsKey(key)) {
            return values.get(key);
        }
        if (key.startsWith("parameters.")) {
            Object parameters = values.get("parameters");
            if (parameters instanceof Map<?, ?> map) {
                return map.get(key.substring("parameters.".length()));
            }
        }
        int dot = key.indexOf('.');
        if (dot > 0) {
            Object nested = values.get(key.substring(0, dot));
            if (nested instanceof Map<?, ?> map) {
                return map.get(key.substring(dot + 1));
            }
        }
        return null;
    }
}
