package io.osb.api.dto.admin;

public record GitClientInstanceDto(
        String id,
        String name,
        String description,
        String remoteUrl,
        String defaultBranch,
        String authMethod,
        String username,
        boolean secretConfigured,
        boolean passphraseConfigured,
        boolean enabled) {}
