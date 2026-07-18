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
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
        String username = resolveUsername(authorization);
        return catalogMapper.toResponse(getCatalogForPlatformUseCase.execute(username));
    }

    private String resolveUsername(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new IllegalArgumentException("Basic authentication required");
        }
        if (!authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            throw new IllegalArgumentException("Basic authentication required");
        }
        String encoded = authorization.substring(6).trim();
        String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        int separator = decoded.indexOf(':');
        if (separator < 0) {
            throw new IllegalArgumentException("invalid Basic credentials");
        }
        String username = decoded.substring(0, separator);
        String password = decoded.substring(separator + 1);
        PlatformPrincipal principal = platformAuthenticator.authenticate(username, password);
        return principal.clientId();
    }
}
