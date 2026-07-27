package io.osb.api.resource.osb;

import io.osb.api.dto.osb.CatalogResponse;
import io.osb.api.mapper.CatalogMapper;
import io.osb.application.catalog.GetCatalogForPlatformUseCase;
import io.osb.auth.PlatformAuthenticator;
import io.osb.auth.PlatformPrincipal;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

@Path("/v2/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogResource {

    private final GetCatalogForPlatformUseCase getCatalogForPlatformUseCase;
    private final PlatformAuthenticator platformAuthenticator;
    private final CatalogMapper catalogMapper;

    public CatalogResource(
            GetCatalogForPlatformUseCase getCatalogForPlatformUseCase,
            PlatformAuthenticator platformAuthenticator,
            CatalogMapper catalogMapper) {
        this.getCatalogForPlatformUseCase = getCatalogForPlatformUseCase;
        this.platformAuthenticator = platformAuthenticator;
        this.catalogMapper = catalogMapper;
    }

    @GET
    public CatalogResponse getCatalog(
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            @HeaderParam("X-Broker-API-Version") String apiVersion) {
        // Skeleton: version header is accepted but not enforced yet.
        PlatformPrincipal principal =
                PlatformBasicAuth.requirePrincipal(platformAuthenticator, authorization);
        return catalogMapper.toResponse(getCatalogForPlatformUseCase.execute(principal.username()));
    }
}
