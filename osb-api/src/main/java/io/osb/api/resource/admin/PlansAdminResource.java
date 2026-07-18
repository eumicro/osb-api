package io.osb.api.resource.admin;

import io.osb.api.dto.osb.ServiceOfferingDto;
import io.osb.api.dto.osb.ServicePlanDto;
import io.osb.api.mapper.CatalogMapper;
import io.osb.application.catalog.GetCatalogUseCase;
import io.osb.domain.catalog.Catalog;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Convenience read. Prefer catalog-scoped admin APIs.
 */
@Path("/api/admin/plans")
@Produces(MediaType.APPLICATION_JSON)
public class PlansAdminResource {

    private final GetCatalogUseCase getCatalogUseCase;
    private final CatalogMapper catalogMapper;

    public PlansAdminResource(GetCatalogUseCase getCatalogUseCase, CatalogMapper catalogMapper) {
        this.getCatalogUseCase = getCatalogUseCase;
        this.catalogMapper = catalogMapper;
    }

    @GET
    public List<PlanRow> list(
            @QueryParam("catalogId") String catalogId,
            @QueryParam("serviceId") String serviceId) {
        Catalog catalog = (catalogId == null || catalogId.isBlank())
                ? getCatalogUseCase.firstOrEmpty()
                : getCatalogUseCase.execute(catalogId);
        List<ServiceOfferingDto> offerings = catalogMapper.toResponse(catalog).services();
        List<PlanRow> rows = new ArrayList<>();
        for (ServiceOfferingDto offering : offerings) {
            if (serviceId != null && !serviceId.isBlank() && !offering.id().equals(serviceId)) {
                continue;
            }
            for (ServicePlanDto plan : offering.plans()) {
                rows.add(new PlanRow(offering.id(), offering.name(), plan));
            }
        }
        return rows;
    }

    public record PlanRow(String serviceId, String serviceName, ServicePlanDto plan) {
    }
}
