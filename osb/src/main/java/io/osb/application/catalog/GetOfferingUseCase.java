package io.osb.application.catalog;

import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.catalog.ServiceOffering;
import java.util.Objects;

public final class GetOfferingUseCase {

    private final CatalogRepository catalogRepository;

    public GetOfferingUseCase(CatalogRepository catalogRepository) {
        this.catalogRepository = Objects.requireNonNull(catalogRepository, "catalogRepository");
    }

    public ServiceOffering execute(String catalogId, String offeringId) {
        return catalogRepository
                .findCatalog(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("catalog not found: " + catalogId))
                .findOffering(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("offering not found: " + offeringId));
    }
}
