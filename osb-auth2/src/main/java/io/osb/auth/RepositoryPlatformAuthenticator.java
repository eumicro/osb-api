package io.osb.auth;

import io.osb.domain.platforms.PlatformClient;
import io.osb.domain.platforms.PlatformClientRepository;
import io.osb.domain.secrets.SecretResolver;
import io.osb.domain.secrets.SecretStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * Authenticates platform clients against {@link PlatformClientRepository} + {@link SecretStore}.
 */
@ApplicationScoped
public class RepositoryPlatformAuthenticator implements PlatformAuthenticator {

    private final PlatformClientRepository platformClientRepository;
    private final SecretStore secretStore;

    public RepositoryPlatformAuthenticator(
            PlatformClientRepository platformClientRepository, SecretStore secretStore) {
        this.platformClientRepository =
                Objects.requireNonNull(platformClientRepository, "platformClientRepository");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore");
    }

    @Override
    public PlatformPrincipal authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            throw new PlatformAuthenticationException("unauthorized");
        }
        PlatformClient platform = platformClientRepository
                .findByUsername(username.trim())
                .orElseThrow(() -> new PlatformAuthenticationException("unauthorized"));
        if (!platform.enabled()) {
            throw new PlatformAuthenticationException("unauthorized");
        }
        if (!platform.hasPassword()) {
            throw new PlatformAuthenticationException("unauthorized");
        }
        String expected;
        try {
            expected = SecretResolver.resolve(secretStore, platform.passwordRef());
        } catch (RuntimeException ex) {
            throw new PlatformAuthenticationException("unauthorized");
        }
        if (!constantTimeEquals(expected, password)) {
            throw new PlatformAuthenticationException("unauthorized");
        }
        return new PlatformPrincipal(
                platform.id(), platform.username(), platform.displayName(), platform.catalogId());
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
