package io.osb.domain.templates;

import java.util.Objects;

/**
 * Independently managed text template with placeholders (e.g. {@code ${instanceId}},
 * {@code ${parameters.storage_gb}}). Not bound to a concrete client instance.
 * Workflows reference templates; engines evaluate placeholders at runtime.
 */
public final class Template {

    private final String id;
    private final String name;
    private final String description;
    private final TemplateKind kind;
    private final String content;
    private final boolean enabled;

    public Template(
            String id,
            String name,
            String description,
            TemplateKind kind,
            String content,
            boolean enabled) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = Objects.requireNonNullElse(description, "");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.content = Objects.requireNonNullElse(content, "");
        this.enabled = enabled;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public TemplateKind kind() {
        return kind;
    }

    /** Raw template text with placeholders. */
    public String content() {
        return content;
    }

    public boolean enabled() {
        return enabled;
    }

    public Template withDetails(
            String name, String description, TemplateKind kind, String content, boolean enabled) {
        return new Template(id, name, description, kind, content, enabled);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
