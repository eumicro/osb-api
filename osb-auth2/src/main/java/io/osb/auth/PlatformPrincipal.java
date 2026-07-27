package io.osb.auth;

/**
 * Authenticated OSB platform client.
 *
 * @param clientId platform entity id (stable identity for instance ownership)
 * @param username Basic-Auth username
 * @param displayName human-readable name
 * @param catalogId catalog scoped to this platform
 */
public record PlatformPrincipal(
        String clientId, String username, String displayName, String catalogId) {}
