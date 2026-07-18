package io.osb.application.catalog;

import io.osb.domain.catalog.CatalogRepository;
import java.util.Objects;

public final class DeleteCatalogUseCase {

    private final CatalogRepository catalogRepository;

    public DeleteCatalogUseCase(CatalogRepository catalogRepository) {
        this.catalogRepository = Objects.requireNonNull(catalogRepository, "catalogRepository");
    }

    public void execute(String catalogId) {
        if (!catalogRepository.deleteCatalog(catalogId)) {
            throw new IllegalArgumentException("catalog not found: " + catalogId);
        }
    }
}
