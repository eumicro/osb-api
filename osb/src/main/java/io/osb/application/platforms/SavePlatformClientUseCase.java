package io.osb.application.platforms;

import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.platforms.PlatformClient;
import io.osb.domain.platforms.PlatformClientRepository;
import io.osb.domain.secrets.SecretRefs;
import io.osb.domain.secrets.SecretStore;
import java.util.Objects;
import java.util.UUID;

public final class SavePlatformClientUseCase {

    private final PlatformClientRepository platformClientRepository;
    private final CatalogRepository catalogRepository;
    private final SecretStore secretStore;

    public SavePlatformClientUseCase(
            PlatformClientRepository platformClientRepository,
            CatalogRepository catalogRepository,
            SecretStore secretStore) {
        this.platformClientRepository =
                Objects.requireNonNull(platformClientRepository, "platformClientRepository");
        this.catalogRepository = Objects.requireNonNull(catalogRepository, "catalogRepository");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore");
    }

    public PlatformClient create(
            String displayName,
            String username,
            String catalogId,
            String password,
            boolean enabled) {
        requireCatalog(catalogId);
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        platformClientRepository
                .findByUsername(username)
                .ifPresent(other -> {
                    throw new IllegalArgumentException("username already in use: " + username);
                });
        String id = "platform-" + UUID.randomUUID().toString().substring(0, 8);
        String passwordRef = SecretRefs.platformPassword(id);
        secretStore.put(passwordRef, password);
        PlatformClient created =
                new PlatformClient(id, displayName, username, catalogId, passwordRef, enabled);
        platformClientRepository.save(created);
        return created;
    }

    public PlatformClient update(
            String id,
            String displayName,
            String username,
            String catalogId,
            String password,
            boolean keepExistingPassword,
            boolean enabled) {
        requireCatalog(catalogId);
        PlatformClient existing = platformClientRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("platform client not found: " + id));
        platformClientRepository
                .findByUsername(username)
                .filter(other -> !other.id().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("username already in use: " + username);
                });
        String passwordRef;
        if (keepExistingPassword || password == null || password.isBlank()) {
            passwordRef = existing.passwordRef();
            if (passwordRef == null || passwordRef.isBlank()) {
                throw new IllegalArgumentException("password is required");
            }
        } else {
            passwordRef = (existing.passwordRef() != null && SecretRefs.isRef(existing.passwordRef()))
                    ? existing.passwordRef()
                    : SecretRefs.platformPassword(id);
            secretStore.put(passwordRef, password);
        }
        PlatformClient updated =
                existing.withDetails(displayName, username, catalogId, passwordRef, enabled);
        platformClientRepository.save(updated);
        return updated;
    }

    private void requireCatalog(String catalogId) {
        catalogRepository
                .findCatalog(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("catalog not found: " + catalogId));
    }
}
