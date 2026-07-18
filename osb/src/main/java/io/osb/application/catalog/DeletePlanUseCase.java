package io.osb.application.catalog;

import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.catalog.ServiceOffering;
import java.util.Objects;

public final class DeletePlanUseCase {

    private final CatalogRepository catalogRepository;

    public DeletePlanUseCase(CatalogRepository catalogRepository) {
        this.catalogRepository = Objects.requireNonNull(catalogRepository, "catalogRepository");
    }

    public ServiceOffering execute(String catalogId, String offeringId, String planId) {
        Catalog catalog = catalogRepository
                .findCatalog(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("catalog not found: " + catalogId));
        ServiceOffering offering = catalog
                .findOffering(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("offering not found: " + offeringId));
        ServiceOffering updated = offering.withoutPlan(planId);
        catalogRepository.saveCatalog(catalog.withReplacedOffering(updated));
        return updated;
    }
}
