package io.osb.api.resource.osb;

import io.osb.auth.PlatformAuthenticationException;
import io.osb.auth.PlatformAuthenticator;
import io.osb.auth.PlatformPrincipal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Shared Basic-Auth resolution for OSB {@code /v2} resources. */
final class PlatformBasicAuth {

    private PlatformBasicAuth() {}

    static PlatformPrincipal requirePrincipal(
            PlatformAuthenticator authenticator, String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new PlatformAuthenticationException("unauthorized");
        }
        if (!authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            throw new PlatformAuthenticationException("unauthorized");
        }
        String encoded = authorization.substring(6).trim();
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new PlatformAuthenticationException("unauthorized");
        }
        int separator = decoded.indexOf(':');
        if (separator < 0) {
            throw new PlatformAuthenticationException("unauthorized");
        }
        String username = decoded.substring(0, separator);
        String password = decoded.substring(separator + 1);
        return authenticator.authenticate(username, password);
    }
}
