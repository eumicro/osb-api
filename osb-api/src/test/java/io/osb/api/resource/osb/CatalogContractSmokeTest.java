package io.osb.api.resource.osb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.osb.api.dto.osb.CatalogResponse;
import io.osb.api.error.PlatformAuthenticationExceptionMapper;
import io.osb.api.mapper.CatalogMapper;
import io.osb.application.catalog.GetCatalogForPlatformUseCase;
import io.osb.auth.PlatformAuthenticationException;
import io.osb.auth.PlatformAuthenticator;
import io.osb.auth.PlatformPrincipal;
import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.catalog.ServiceOffering;
import io.osb.domain.catalog.ServicePlan;
import io.osb.domain.platforms.PlatformClient;
import io.osb.domain.platforms.PlatformClientRepository;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Minimal OSB catalog contract smoke: Basic auth required (401), valid client gets assigned catalog.
 */
class CatalogContractSmokeTest {

    private CatalogResource catalogResource;
    private PlatformAuthenticationExceptionMapper unauthorizedMapper;

    @BeforeEach
    void setUp() {
        MapCatalogRepo catalogs = new MapCatalogRepo();
        MapPlatformRepo platforms = new MapPlatformRepo();
        catalogs.saveCatalog(catalog(
                "catalog-a",
                offering("svc-a", "plan-a")));
        platforms.save(new PlatformClient(
                "platform-a",
                "Platform A",
                "user-a",
                "catalog-a",
                "osb/platform/platform-a/password",
                true));

        PlatformAuthenticator authenticator = (username, password) -> {
            if ("user-a".equals(username) && "pass-a".equals(password)) {
                return new PlatformPrincipal("platform-a", "user-a", "Platform A", "catalog-a");
            }
            throw new PlatformAuthenticationException("unauthorized");
        };

        catalogResource = new CatalogResource(
                new GetCatalogForPlatformUseCase(platforms, catalogs),
                authenticator,
                new CatalogMapper());
        unauthorizedMapper = new PlatformAuthenticationExceptionMapper();
    }

    @Test
    void missingAuthMapsTo401() {
        PlatformAuthenticationException ex = assertThrows(
                PlatformAuthenticationException.class,
                () -> catalogResource.getCatalog(null, "2.17"));
        Response response = unauthorizedMapper.toResponse(ex);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertTrue(response.getHeaderString("WWW-Authenticate").startsWith("Basic"));
    }

    @Test
    void invalidBasicAuthMapsTo401() {
        PlatformAuthenticationException ex = assertThrows(
                PlatformAuthenticationException.class,
                () -> catalogResource.getCatalog(basic("user-a", "wrong"), "2.17"));
        assertEquals(
                Response.Status.UNAUTHORIZED.getStatusCode(),
                unauthorizedMapper.toResponse(ex).getStatus());
    }

    @Test
    void validBasicAuthReturnsAssignedCatalogOfferings() {
        CatalogResponse catalog = catalogResource.getCatalog(basic("user-a", "pass-a"), "2.17");
        Set<String> ids = catalog.services().stream().map(s -> s.id()).collect(Collectors.toSet());
        assertEquals(Set.of("svc-a"), ids);
    }

    private static String basic(String username, String password) {
        String token = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private static Catalog catalog(String id, ServiceOffering offering) {
        return new Catalog(id, id, "desc", List.of(offering));
    }

    private static ServiceOffering offering(String serviceId, String planId) {
        return new ServiceOffering(
                serviceId,
                serviceId,
                "desc",
                false,
                List.of(new ServicePlan(planId, planId, "desc", true, false, Map.of(), Map.of())));
    }

    private static final class MapCatalogRepo implements CatalogRepository {
        private final Map<String, Catalog> byId = new HashMap<>();

        @Override
        public List<Catalog> listCatalogs() {
            return List.copyOf(byId.values());
        }

        @Override
        public Optional<Catalog> findCatalog(String catalogId) {
            return Optional.ofNullable(byId.get(catalogId));
        }

        @Override
        public void saveCatalog(Catalog catalog) {
            byId.put(catalog.id(), catalog);
        }

        @Override
        public boolean deleteCatalog(String catalogId) {
            return byId.remove(catalogId) != null;
        }
    }

    private static final class MapPlatformRepo implements PlatformClientRepository {
        private final Map<String, PlatformClient> byId = new HashMap<>();

        @Override
        public List<PlatformClient> list() {
            return List.copyOf(byId.values());
        }

        @Override
        public Optional<PlatformClient> findById(String id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<PlatformClient> findByUsername(String username) {
            return byId.values().stream().filter(p -> p.username().equals(username)).findFirst();
        }

        @Override
        public void save(PlatformClient platformClient) {
            byId.put(platformClient.id(), platformClient);
        }

        @Override
        public boolean delete(String id) {
            return byId.remove(id) != null;
        }
    }
}
