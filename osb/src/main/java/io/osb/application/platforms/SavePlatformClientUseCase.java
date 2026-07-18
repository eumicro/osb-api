package io.osb.application.platforms;

import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.platforms.PlatformClient;
import io.osb.domain.platforms.PlatformClientRepository;
import java.util.Objects;
import java.util.UUID;

public final class SavePlatformClientUseCase {

    private final PlatformClientRepository platformClientRepository;
    private final CatalogRepository catalogRepository;

    public SavePlatformClientUseCase(
            PlatformClientRepository platformClientRepository,
            CatalogRepository catalogRepository) {
        this.platformClientRepository =
                Objects.requireNonNull(platformClientRepository, "platformClientRepository");
        this.catalogRepository = Objects.requireNonNull(catalogRepository, "catalogRepository");
    }

    public PlatformClient create(
            String displayName,
            String username,
            String catalogId,
            boolean enabled) {
        requireCatalog(catalogId);
        PlatformClient created = new PlatformClient(
                "platform-" + UUID.randomUUID().toString().substring(0, 8),
                displayName,
                username,
                catalogId,
                enabled);
        platformClientRepository.save(created);
        return created;
    }

    public PlatformClient update(
            String id,
            String displayName,
            String username,
            String catalogId,
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
        PlatformClient updated = existing.withDetails(displayName, username, catalogId, enabled);
        platformClientRepository.save(updated);
        return updated;
    }

    private void requireCatalog(String catalogId) {
        catalogRepository
                .findCatalog(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("catalog not found: " + catalogId));
    }
}
