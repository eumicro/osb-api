package io.osb.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.osb.domain.platforms.PlatformClient;
import io.osb.domain.platforms.PlatformClientRepository;
import io.osb.domain.secrets.SecretRefs;
import io.osb.domain.secrets.SecretStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RepositoryPlatformAuthenticatorTest {

    private InMemoryStore store;
    private MapPlatformRepo repo;
    private RepositoryPlatformAuthenticator authenticator;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        repo = new MapPlatformRepo();
        authenticator = new RepositoryPlatformAuthenticator(repo, store);
    }

    @Test
    void authenticatesValidCredentials() {
        String ref = SecretRefs.platformPassword("platform-1");
        store.put(ref, "s3cret");
        repo.save(new PlatformClient("platform-1", "CF", "cf-broker", "default", ref, true));

        PlatformPrincipal principal = authenticator.authenticate("cf-broker", "s3cret");

        assertEquals("platform-1", principal.clientId());
        assertEquals("cf-broker", principal.username());
        assertEquals("default", principal.catalogId());
    }

    @Test
    void rejectsWrongPassword() {
        String ref = SecretRefs.platformPassword("platform-1");
        store.put(ref, "s3cret");
        repo.save(new PlatformClient("platform-1", "CF", "cf-broker", "default", ref, true));

        assertThrows(
                PlatformAuthenticationException.class,
                () -> authenticator.authenticate("cf-broker", "wrong"));
    }

    @Test
    void rejectsDisabledClient() {
        String ref = SecretRefs.platformPassword("platform-1");
        store.put(ref, "s3cret");
        repo.save(new PlatformClient("platform-1", "CF", "cf-broker", "default", ref, false));

        assertThrows(
                PlatformAuthenticationException.class,
                () -> authenticator.authenticate("cf-broker", "s3cret"));
    }

    @Test
    void rejectsUnknownUserWithoutEnumerationDifference() {
        assertThrows(
                PlatformAuthenticationException.class,
                () -> authenticator.authenticate("missing", "anything"));
    }

    private static final class InMemoryStore implements SecretStore {
        private final Map<String, String> values = new HashMap<>();

        @Override
        public void put(String ref, String value) {
            values.put(ref, value);
        }

        @Override
        public Optional<String> get(String ref) {
            return Optional.ofNullable(values.get(ref));
        }

        @Override
        public void delete(String ref) {
            values.remove(ref);
        }
    }

    private static final class MapPlatformRepo implements PlatformClientRepository {
        private final Map<String, PlatformClient> byId = new HashMap<>();

        @Override
        public List<PlatformClient> list() {
            return List.copyOf(byId.values());
        }

        @Override
        public Optional<PlatformClient> findById(String id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<PlatformClient> findByUsername(String username) {
            return byId.values().stream().filter(p -> p.username().equals(username)).findFirst();
        }

        @Override
        public void save(PlatformClient platformClient) {
            byId.put(platformClient.id(), platformClient);
        }

        @Override
        public boolean delete(String id) {
            return byId.remove(id) != null;
        }
    }
}
