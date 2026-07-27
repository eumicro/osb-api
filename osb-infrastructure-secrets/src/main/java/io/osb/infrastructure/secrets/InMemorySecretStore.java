package io.osb.infrastructure.secrets;

import io.osb.domain.secrets.SecretStore;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Process-local secret store for tests and local {@code quarkus:dev} without OpenBao.
 */
public final class InMemorySecretStore implements SecretStore {

    private final ConcurrentMap<String, String> values = new ConcurrentHashMap<>();

    @Override
    public void put(String ref, String value) {
        if (ref == null || ref.isBlank()) {
            throw new IllegalArgumentException("ref must not be blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        values.put(ref, value);
    }

    @Override
    public Optional<String> get(String ref) {
        if (ref == null || ref.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(ref));
    }

    @Override
    public void delete(String ref) {
        if (ref != null && !ref.isBlank()) {
            values.remove(ref);
        }
    }
}
