package io.osb.api.resource.osb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.osb.api.admin.AdminStore;
import io.osb.api.dto.osb.CatalogResponse;
import io.osb.api.dto.osb.ProvisionServiceInstanceRequest;
import io.osb.api.mapper.CatalogMapper;
import io.osb.application.catalog.GetCatalogForPlatformUseCase;
import io.osb.auth.PlatformAuthenticator;
import io.osb.auth.PlatformPrincipal;
import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.catalog.ServiceOffering;
import io.osb.domain.catalog.ServicePlan;
import io.osb.domain.platforms.PlatformClient;
import io.osb.domain.platforms.PlatformClientRepository;
import io.osb.workflow.DeprovisioningCommand;
import io.osb.workflow.DeprovisioningWorkflow;
import io.osb.workflow.ProvisioningCommand;
import io.osb.workflow.ProvisioningWorkflow;
import io.osb.workflow.WorkflowStatus;
import jakarta.ws.rs.NotFoundException;
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
 * Ensures one platform client cannot reach another platform's catalog offerings or instances
 * (including via a known-valid foreign instance URL).
 */
class PlatformClientIsolationTest {

    private static final PlatformPrincipal PLATFORM_A =
            new PlatformPrincipal("platform-a", "user-a", "Platform A", "catalog-a");
    private static final PlatformPrincipal PLATFORM_B =
            new PlatformPrincipal("platform-b", "user-b", "Platform B", "catalog-b");

    private MapCatalogRepo catalogs;
    private MapPlatformRepo platforms;
    private AdminStore adminStore;
    private CatalogResource catalogResource;
    private ServiceInstancesResource instancesResource;

    @BeforeEach
    void setUp() {
        catalogs = new MapCatalogRepo();
        platforms = new MapPlatformRepo();

        catalogs.saveCatalog(catalog("catalog-a", offering("svc-a", "plan-a")));
        catalogs.saveCatalog(catalog("catalog-b", offering("svc-b", "plan-b")));

        platforms.save(new PlatformClient(
                "platform-a",
                "Platform A",
                "user-a",
                "catalog-a",
                "osb/platform/platform-a/password",
                true));
        platforms.save(new PlatformClient(
                "platform-b",
                "Platform B",
                "user-b",
                "catalog-b",
                "osb/platform/platform-b/password",
                true));

        PlatformAuthenticator authenticator = (username, password) -> {
            if ("user-a".equals(username) && "pass-a".equals(password)) {
                return PLATFORM_A;
            }
            if ("user-b".equals(username) && "pass-b".equals(password)) {
                return PLATFORM_B;
            }
            throw new io.osb.auth.PlatformAuthenticationException("unauthorized");
        };

        adminStore = new AdminStore(new StubProvisioningWorkflow(), new StubDeprovisioningWorkflow());
        CatalogMapper mapper = new CatalogMapper();
        GetCatalogForPlatformUseCase getCatalog =
                new GetCatalogForPlatformUseCase(platforms, catalogs);
        catalogResource = new CatalogResource(getCatalog, authenticator, mapper);
        instancesResource = new ServiceInstancesResource(authenticator, adminStore, catalogs);

        // Valid instance owned by platform A — B will call the same URL.
        adminStore.provision("inst-owned-by-a", "svc-a", "plan-a", "platform-a", Map.of());
    }

    @Test
    void catalogEndpointReturnsOnlyAssignedCatalogOfferings() {
        CatalogResponse forA = catalogResource.getCatalog(basic("user-a", "pass-a"), "2.17");
        CatalogResponse forB = catalogResource.getCatalog(basic("user-b", "pass-b"), "2.17");

        assertEquals(Set.of("svc-a"), offeringIds(forA));
        assertEquals(Set.of("svc-b"), offeringIds(forB));
        assertTrue(offeringIds(forA).stream().noneMatch(id -> id.equals("svc-b")));
        assertTrue(offeringIds(forB).stream().noneMatch(id -> id.equals("svc-a")));
    }

    @Test
    void cannotProvisionOfferingFromForeignCatalog() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> instancesResource.provision(
                        "inst-foreign-offering",
                        basic("user-a", "pass-a"),
                        "2.17",
                        true,
                        new ProvisionServiceInstanceRequest("svc-b", "plan-b", Map.of(), null)));

        assertTrue(ex.getMessage().contains("service not in platform catalog"));
    }

    @Test
    void getForeignInstanceUrlReturnsNotFound() {
        assertThrows(
                NotFoundException.class,
                () -> instancesResource.getInstance(
                        "inst-owned-by-a", basic("user-b", "pass-b"), "2.17"));
    }

    @Test
    void lastOperationOnForeignInstanceUrlReturnsNotFound() {
        assertThrows(
                NotFoundException.class,
                () -> instancesResource.lastOperation(
                        "inst-owned-by-a",
                        basic("user-b", "pass-b"),
                        "2.17",
                        "svc-a",
                        "plan-a",
                        null));
    }

    @Test
    void deleteForeignInstanceUrlReturnsNotFound() {
        assertThrows(
                NotFoundException.class,
                () -> instancesResource.deprovision(
                        "inst-owned-by-a",
                        basic("user-b", "pass-b"),
                        "2.17",
                        "svc-a",
                        "plan-a",
                        true));
    }

    @Test
    void putOnForeignInstanceUrlReturnsNotFound() {
        assertThrows(
                NotFoundException.class,
                () -> instancesResource.provision(
                        "inst-owned-by-a",
                        basic("user-b", "pass-b"),
                        "2.17",
                        true,
                        new ProvisionServiceInstanceRequest("svc-b", "plan-b", Map.of(), null)));
    }

    @Test
    void ownerCanStillReadOwnInstance() {
        Response response =
                instancesResource.getInstance("inst-owned-by-a", basic("user-a", "pass-a"), "2.17");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    private static Set<String> offeringIds(CatalogResponse response) {
        return response.services().stream().map(s -> s.id()).collect(Collectors.toSet());
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

    private static final class StubProvisioningWorkflow implements ProvisioningWorkflow {
        @Override
        public String start(ProvisioningCommand command) {
            return "op-prov-" + command.instanceId();
        }

        @Override
        public WorkflowStatus status(String operationId) {
            return WorkflowStatus.IN_PROGRESS;
        }
    }

    private static final class StubDeprovisioningWorkflow implements DeprovisioningWorkflow {
        @Override
        public String start(DeprovisioningCommand command) {
            return "op-deprov-" + command.instanceId();
        }

        @Override
        public WorkflowStatus status(String operationId) {
            return WorkflowStatus.IN_PROGRESS;
        }
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
