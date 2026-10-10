package io.osb.api.resource.osb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.osb.api.admin.AdminStore;
import io.osb.api.admin.BindingStore;
import io.osb.api.admin.MapServiceBindingRecordStore;
import io.osb.api.admin.MapServiceInstanceRecordStore;
import io.osb.api.dto.osb.BindServiceBindingRequest;
import io.osb.auth.PlatformAuthenticator;
import io.osb.auth.PlatformPrincipal;
import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.catalog.ServiceOffering;
import io.osb.domain.catalog.ServicePlan;
import io.osb.workflow.BindingCommand;
import io.osb.workflow.BindingWorkflow;
import io.osb.workflow.DeprovisioningCommand;
import io.osb.workflow.DeprovisioningWorkflow;
import io.osb.workflow.ProvisioningCommand;
import io.osb.workflow.ProvisioningWorkflow;
import io.osb.workflow.UnbindingCommand;
import io.osb.workflow.UnbindingWorkflow;
import io.osb.workflow.UpdatingCommand;
import io.osb.workflow.UpdatingWorkflow;
import io.osb.workflow.WorkflowStatus;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceBindingsResourceTest {

    private ServiceBindingsResource resource;
    private AdminStore adminStore;

    @BeforeEach
    void setUp() {
        MapCatalogRepo catalogs = new MapCatalogRepo();
        catalogs.saveCatalog(new Catalog(
                "catalog-a",
                "catalog-a",
                "desc",
                List.of(new ServiceOffering(
                        "svc-a",
                        "svc-a",
                        "desc",
                        true,
                        List.of(new ServicePlan(
                                "plan-a", "plan-a", "desc", true, true, Map.of(), Map.of()))))));

        PlatformAuthenticator authenticator = (username, password) -> {
            if ("user-a".equals(username) && "pass-a".equals(password)) {
                return new PlatformPrincipal("platform-a", "user-a", "Platform A", "catalog-a");
            }
            throw new io.osb.auth.PlatformAuthenticationException("unauthorized");
        };

        adminStore = new AdminStore(
                succeededProvisioning(),
                new DeprovisioningWorkflow() {
                    @Override
                    public String start(DeprovisioningCommand command) {
                        return "op-d";
                    }

                    @Override
                    public WorkflowStatus status(String operationId) {
                        return WorkflowStatus.SUCCEEDED;
                    }
                },
                new UpdatingWorkflow() {
                    @Override
                    public String start(UpdatingCommand command) {
                        return "op-u";
                    }

                    @Override
                    public WorkflowStatus status(String operationId) {
                        return WorkflowStatus.SUCCEEDED;
                    }
                },
                new MapServiceInstanceRecordStore());
        adminStore.provision("inst-1", "svc-a", "plan-a", "platform-a", Map.of());

        BindingStore bindingStore = new BindingStore(
                new BindingWorkflow() {
                    @Override
                    public String start(BindingCommand command) {
                        return "op-bind";
                    }

                    @Override
                    public WorkflowStatus status(String operationId) {
                        return WorkflowStatus.SUCCEEDED;
                    }
                },
                new UnbindingWorkflow() {
                    @Override
                    public String start(UnbindingCommand command) {
                        return "op-unbind";
                    }

                    @Override
                    public WorkflowStatus status(String operationId) {
                        return WorkflowStatus.SUCCEEDED;
                    }
                },
                new MapServiceBindingRecordStore());

        resource = new ServiceBindingsResource(authenticator, adminStore, bindingStore, catalogs);
    }

    @Test
    void bindAsyncReturnsAccepted() {
        Response response = resource.bind(
                "inst-1",
                "bind-1",
                basic("user-a", "pass-a"),
                "2.17",
                true,
                new BindServiceBindingRequest("svc-a", "plan-a", Map.of(), null, null));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
    }

    @Test
    void bindRequiresAsync() {
        Response response = resource.bind(
                "inst-1",
                "bind-2",
                basic("user-a", "pass-a"),
                "2.17",
                false,
                new BindServiceBindingRequest("svc-a", "plan-a", Map.of(), null, null));
        assertEquals(422, response.getStatus());
    }

    @Test
    void foreignInstanceNotFound() {
        assertThrows(
                NotFoundException.class,
                () -> resource.bind(
                        "missing",
                        "bind-1",
                        basic("user-a", "pass-a"),
                        "2.17",
                        true,
                        new BindServiceBindingRequest("svc-a", "plan-a", Map.of(), null, null)));
    }

    private static ProvisioningWorkflow succeededProvisioning() {
        return new ProvisioningWorkflow() {
            @Override
            public String start(ProvisioningCommand command) {
                return "op-p";
            }

            @Override
            public WorkflowStatus status(String operationId) {
                return WorkflowStatus.SUCCEEDED;
            }
        };
    }

    private static String basic(String username, String password) {
        String token = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
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
}
