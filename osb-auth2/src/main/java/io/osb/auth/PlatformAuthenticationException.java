package io.osb.auth;

/**
 * Thrown when platform Basic Auth fails (maps to HTTP 401).
 */
public final class PlatformAuthenticationException extends RuntimeException {

    public PlatformAuthenticationException(String message) {
        super(message);
    }
}
