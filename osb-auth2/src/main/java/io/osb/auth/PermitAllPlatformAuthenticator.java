package io.osb.auth;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Skeleton authenticator: accepts any non-blank credentials for local browser testing.
 */
@ApplicationScoped
public class PermitAllPlatformAuthenticator implements PlatformAuthenticator {

    @Override
    public PlatformPrincipal authenticate(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username required");
        }
        return new PlatformPrincipal(username, "skeleton-platform-client");
    }
}
