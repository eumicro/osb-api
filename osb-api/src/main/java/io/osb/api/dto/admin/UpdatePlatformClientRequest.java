package io.osb.api.dto.admin;

public record UpdatePlatformClientRequest(
        String displayName,
        String username,
        String catalogId,
        String password,
        boolean enabled) {
}
