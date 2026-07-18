package io.osb.application.catalog;

import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.catalog.ServiceOffering;
import io.osb.domain.catalog.ServicePlan;
import java.util.Objects;

public final class GetPlanUseCase {

    private final CatalogRepository catalogRepository;

    public GetPlanUseCase(CatalogRepository catalogRepository) {
        this.catalogRepository = Objects.requireNonNull(catalogRepository, "catalogRepository");
    }

    public ServicePlan execute(String catalogId, String offeringId, String planId) {
        ServiceOffering offering = catalogRepository
                .findCatalog(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("catalog not found: " + catalogId))
                .findOffering(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("offering not found: " + offeringId));
        return offering.plans().stream()
                .filter(plan -> plan.id().equals(planId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("plan not found: " + planId));
    }
}
