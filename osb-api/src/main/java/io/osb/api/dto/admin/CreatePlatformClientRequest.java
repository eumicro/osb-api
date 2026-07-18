package io.osb.api.dto.admin;

public record CreatePlatformClientRequest(
        String displayName,
        String username,
        String catalogId,
        boolean enabled) {
}
