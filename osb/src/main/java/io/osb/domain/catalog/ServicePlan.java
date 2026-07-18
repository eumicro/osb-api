package io.osb.domain.catalog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Rich domain model: a plan belongs to an offering and may define OSB parameter schemas.
 */
public final class ServicePlan {

    private final String id;
    private final String name;
    private final String description;
    private final boolean free;
    private final boolean bindable;
    /** OSB plan {@code schemas} object (e.g. service_instance.create.parameters). */
    private final Map<String, Object> schemas;
    /** Admin UI schema for provision-parameter forms (not part of OSB catalog). */
    private final Map<String, Object> parametersUiSchema;

    public ServicePlan(
            String id,
            String name,
            String description,
            boolean free,
            boolean bindable,
            Map<String, Object> schemas,
            Map<String, Object> parametersUiSchema) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.free = free;
        this.bindable = bindable;
        this.schemas = copyMap(schemas);
        this.parametersUiSchema = copyMap(parametersUiSchema);
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

    public boolean free() {
        return free;
    }

    public boolean bindable() {
        return bindable;
    }

    public Map<String, Object> schemas() {
        return schemas;
    }

    public Map<String, Object> parametersUiSchema() {
        return parametersUiSchema;
    }

    private static Map<String, Object> copyMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
