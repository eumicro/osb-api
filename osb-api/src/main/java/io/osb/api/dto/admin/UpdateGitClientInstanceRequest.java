package io.osb.api.dto.admin;

public record UpdateGitClientInstanceRequest(
        String name,
        String description,
        String remoteUrl,
        String defaultBranch,
        String authMethod,
        String username,
        String secret,
        String passphrase,
        Boolean enabled) {}
