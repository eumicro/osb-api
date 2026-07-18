package io.osb.api.resource.admin;

import io.osb.api.dto.osb.ServiceOfferingDto;
import io.osb.api.mapper.CatalogMapper;
import io.osb.application.catalog.GetCatalogUseCase;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Convenience read. Prefer {@code /api/admin/catalogs/{catalogId}/offerings}.
 */
@Path("/api/admin/offerings")
@Produces(MediaType.APPLICATION_JSON)
public class OfferingsAdminResource {

    private final GetCatalogUseCase getCatalogUseCase;
    private final CatalogMapper catalogMapper;

    public OfferingsAdminResource(GetCatalogUseCase getCatalogUseCase, CatalogMapper catalogMapper) {
        this.getCatalogUseCase = getCatalogUseCase;
        this.catalogMapper = catalogMapper;
    }

    @GET
    public List<ServiceOfferingDto> list(@QueryParam("catalogId") String catalogId) {
        if (catalogId == null || catalogId.isBlank()) {
            return catalogMapper.toResponse(getCatalogUseCase.firstOrEmpty()).services();
        }
        return catalogMapper.toResponse(getCatalogUseCase.execute(catalogId)).services();
    }
}
