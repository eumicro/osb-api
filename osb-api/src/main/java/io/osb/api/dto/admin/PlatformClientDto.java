package io.osb.api.dto.admin;

public record PlatformClientDto(
        String id,
        String displayName,
        String username,
        String catalogId,
        boolean enabled) {
}
