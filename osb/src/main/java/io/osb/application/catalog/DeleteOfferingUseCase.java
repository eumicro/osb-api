package io.osb.application.catalog;

import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import java.util.Objects;

public final class DeleteOfferingUseCase {

    private final CatalogRepository catalogRepository;

    public DeleteOfferingUseCase(CatalogRepository catalogRepository) {
        this.catalogRepository = Objects.requireNonNull(catalogRepository, "catalogRepository");
    }

    public void execute(String catalogId, String offeringId) {
        Catalog catalog = catalogRepository
                .findCatalog(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("catalog not found: " + catalogId));
        catalogRepository.saveCatalog(catalog.withoutOffering(offeringId));
    }
}
