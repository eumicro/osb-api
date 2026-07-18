package io.osb.application.catalog;

import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.catalog.ServiceOffering;
import io.osb.domain.catalog.ServicePlan;
import java.util.List;
import java.util.Objects;

public final class SaveOfferingUseCase {

    private final CatalogRepository catalogRepository;

    public SaveOfferingUseCase(CatalogRepository catalogRepository) {
        this.catalogRepository = Objects.requireNonNull(catalogRepository, "catalogRepository");
    }

    public ServiceOffering create(
            String catalogId,
            String id,
            String name,
            String description,
            boolean bindable,
            ServicePlan initialPlan) {
        Catalog catalog = requireCatalog(catalogId);
        ServiceOffering created = new ServiceOffering(id, name, description, bindable, List.of(initialPlan));
        catalogRepository.saveCatalog(catalog.withAddedOffering(created));
        return created;
    }

    public ServiceOffering update(
            String catalogId,
            String id,
            String name,
            String description,
            boolean bindable) {
        Catalog catalog = requireCatalog(catalogId);
        ServiceOffering existing = catalog
                .findOffering(id)
                .orElseThrow(() -> new IllegalArgumentException("offering not found: " + id));
        ServiceOffering updated = existing.withDetails(name, description, bindable);
        catalogRepository.saveCatalog(catalog.withReplacedOffering(updated));
        return updated;
    }

    private Catalog requireCatalog(String catalogId) {
        return catalogRepository
                .findCatalog(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("catalog not found: " + catalogId));
    }
}
