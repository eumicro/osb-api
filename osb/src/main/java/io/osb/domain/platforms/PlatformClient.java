package io.osb.domain.platforms;

import java.util.Objects;

/**
 * Platform identity that consumes the OSB API.
 * Cardinality: Catalog 1:N PlatformClient — each platform has exactly one
 * {@code catalogId}; many platforms may share the same catalog for
 * {@code GET /v2/catalog}.
 */
public final class PlatformClient {

    private final String id;
    private final String displayName;
    private final String username;
    private final String catalogId;
    private final boolean enabled;

    public PlatformClient(
            String id,
            String displayName,
            String username,
            String catalogId,
            boolean enabled) {
        this.id = requireText(id, "id");
        this.displayName = requireText(displayName, "displayName");
        this.username = requireText(username, "username");
        this.catalogId = requireText(catalogId, "catalogId");
        this.enabled = enabled;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String username() {
        return username;
    }

    public String catalogId() {
        return catalogId;
    }

    public boolean enabled() {
        return enabled;
    }

    public PlatformClient withDetails(
            String displayName,
            String username,
            String catalogId,
            boolean enabled) {
        return new PlatformClient(id, displayName, username, catalogId, enabled);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
