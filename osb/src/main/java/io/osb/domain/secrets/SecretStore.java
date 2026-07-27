package io.osb.domain.secrets;

import java.util.Optional;

/**
 * Port for storing client credentials outside the relational database.
 * Values are addressed by opaque references ({@link SecretRefs}).
 */
public interface SecretStore {

    void put(String ref, String value);

    Optional<String> get(String ref);

    void delete(String ref);

    default boolean exists(String ref) {
        return get(ref).isPresent();
    }
}
