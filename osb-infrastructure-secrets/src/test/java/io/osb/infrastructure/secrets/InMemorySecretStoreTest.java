package io.osb.infrastructure.secrets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InMemorySecretStoreTest {

    @Test
    void putGetDelete() {
        InMemorySecretStore store = new InMemorySecretStore();
        store.put("osb/http/a/secret", "s3cret");
        assertEquals("s3cret", store.get("osb/http/a/secret").orElseThrow());
        store.delete("osb/http/a/secret");
        assertTrue(store.get("osb/http/a/secret").isEmpty());
    }
}
