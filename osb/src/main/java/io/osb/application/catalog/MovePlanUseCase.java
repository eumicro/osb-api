package io.osb.application.catalog;

import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.catalog.ServiceOffering;
import io.osb.domain.catalog.ServicePlan;
import java.util.Objects;

public final class MovePlanUseCase {

    private final CatalogRepository catalogRepository;

    public MovePlanUseCase(CatalogRepository catalogRepository) {
        this.catalogRepository = Objects.requireNonNull(catalogRepository, "catalogRepository");
    }

    public ServiceOffering execute(
            String catalogId,
            String sourceOfferingId,
            String planId,
            String targetOfferingId) {
        requireText(catalogId, "catalogId");
        requireText(sourceOfferingId, "sourceOfferingId");
        requireText(planId, "planId");
        requireText(targetOfferingId, "targetOfferingId");
        if (sourceOfferingId.equals(targetOfferingId)) {
            return requireOffering(requireCatalog(catalogId), sourceOfferingId);
        }

        Catalog catalog = requireCatalog(catalogId);
        ServiceOffering source = requireOffering(catalog, sourceOfferingId);
        ServiceOffering target = requireOffering(catalog, targetOfferingId);
        ServicePlan plan = source.plans().stream()
                .filter(item -> item.id().equals(planId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("plan not found: " + planId));
        if (source.plans().size() <= 1) {
            throw new IllegalArgumentException("cannot move last plan of offering: " + sourceOfferingId);
        }
        if (target.plans().stream().anyMatch(item -> item.id().equals(planId))) {
            throw new IllegalArgumentException("plan id already exists on target offering: " + planId);
        }

        ServiceOffering updatedSource = source.withoutPlan(planId);
        ServiceOffering updatedTarget = target.withAddedPlan(plan);
        Catalog updated = catalog
                .withReplacedOffering(updatedSource)
                .withReplacedOffering(updatedTarget);
        catalogRepository.saveCatalog(updated);
        return updatedTarget;
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

    private static void requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
