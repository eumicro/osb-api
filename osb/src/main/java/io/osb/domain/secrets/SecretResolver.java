package io.osb.domain.secrets;

import java.util.Objects;

/**
 * Resolves a stored reference (or legacy plaintext during migration) to the secret value.
 */
public final class SecretResolver {

    private SecretResolver() {}

    public static String resolve(SecretStore store, String refOrPlain) {
        Objects.requireNonNull(store, "store");
        if (refOrPlain == null || refOrPlain.isBlank()) {
            return "";
        }
        if (!SecretRefs.isRef(refOrPlain)) {
            // Dual-read: pre-migration plaintext still in DB.
            return refOrPlain;
        }
        return store.get(refOrPlain)
                .orElseThrow(() -> new IllegalStateException("secret not found for ref: " + refOrPlain));
    }

    public static String resolveOptional(SecretStore store, String refOrPlain) {
        Objects.requireNonNull(store, "store");
        if (refOrPlain == null || refOrPlain.isBlank()) {
            return "";
        }
        if (!SecretRefs.isRef(refOrPlain)) {
            return refOrPlain;
        }
        return store.get(refOrPlain).orElse("");
    }
}
