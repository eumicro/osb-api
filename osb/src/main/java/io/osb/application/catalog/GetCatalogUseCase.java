package io.osb.application.catalog;

import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import java.util.List;
import java.util.Objects;

/**
 * Admin/helper: load catalog by id. OSB platforms use {@link GetCatalogForPlatformUseCase}.
 */
public final class GetCatalogUseCase {

    private final CatalogRepository catalogRepository;

    public GetCatalogUseCase(CatalogRepository catalogRepository) {
        this.catalogRepository = Objects.requireNonNull(catalogRepository, "catalogRepository");
    }

    public Catalog execute(String catalogId) {
        return catalogRepository
                .findCatalog(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("catalog not found: " + catalogId));
    }

    public Catalog firstOrEmpty() {
        List<Catalog> catalogs = catalogRepository.listCatalogs();
        if (catalogs.isEmpty()) {
            return new Catalog("empty", "empty", "", List.of());
        }
        return catalogs.getFirst();
    }
}
