package io.osb.application.catalog;

import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.platforms.PlatformClient;
import io.osb.domain.platforms.PlatformClientRepository;
import java.util.Objects;

/**
 * Resolves the catalog visible to a platform client (OSB {@code GET /v2/catalog}).
 */
public final class GetCatalogForPlatformUseCase {

    private final PlatformClientRepository platformClientRepository;
    private final CatalogRepository catalogRepository;

    public GetCatalogForPlatformUseCase(
            PlatformClientRepository platformClientRepository,
            CatalogRepository catalogRepository) {
        this.platformClientRepository =
                Objects.requireNonNull(platformClientRepository, "platformClientRepository");
        this.catalogRepository = Objects.requireNonNull(catalogRepository, "catalogRepository");
    }

    public Catalog execute(String username) {
        PlatformClient platform = platformClientRepository
                .findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("unknown platform client: " + username));
        if (!platform.enabled()) {
            throw new IllegalArgumentException("platform client disabled: " + username);
        }
        return catalogRepository
                .findCatalog(platform.catalogId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "catalog not found for platform: " + platform.catalogId()));
    }
}
