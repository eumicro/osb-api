package io.osb.application.catalog;

import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.catalog.ServiceOffering;
import io.osb.domain.catalog.ServicePlan;
import java.util.Objects;

public final class SavePlanUseCase {

    private final CatalogRepository catalogRepository;

    public SavePlanUseCase(CatalogRepository catalogRepository) {
        this.catalogRepository = Objects.requireNonNull(catalogRepository, "catalogRepository");
    }

    public ServiceOffering create(String catalogId, String offeringId, ServicePlan plan) {
        Catalog catalog = requireCatalog(catalogId);
        ServiceOffering offering = requireOffering(catalog, offeringId);
        ServiceOffering updated = offering.withAddedPlan(plan);
        catalogRepository.saveCatalog(catalog.withReplacedOffering(updated));
        return updated;
    }

    public ServiceOffering update(String catalogId, String offeringId, ServicePlan plan) {
        Catalog catalog = requireCatalog(catalogId);
        ServiceOffering offering = requireOffering(catalog, offeringId);
        ServiceOffering updated = offering.withReplacedPlan(plan);
        catalogRepository.saveCatalog(catalog.withReplacedOffering(updated));
        return updated;
    }

    private Catalog requireCatalog(String catalogId) {
        return catalogRepository
                .findCatalog(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("catalog not found: " + catalogId));
    }

    private static ServiceOffering requireOffering(Catalog catalog, String offeringId) {
        return catalog
                .findOffering(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("offering not found: " + offeringId));
    }
}
