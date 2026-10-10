package io.osb.api.resource.osb;

import io.osb.api.admin.AdminStore;
import io.osb.api.admin.BindingStore;
import io.osb.api.dto.admin.ServiceInstanceDto;
import io.osb.api.dto.osb.BindServiceBindingRequest;
import io.osb.api.dto.osb.LastOperationResponse;
import io.osb.api.dto.osb.ServiceBindingDto;
import io.osb.auth.PlatformAuthenticator;
import io.osb.auth.PlatformPrincipal;
import io.osb.domain.catalog.Catalog;
import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.catalog.PlanParameterSchemas;
import io.osb.domain.catalog.ServiceOffering;
import io.osb.domain.catalog.ServicePlan;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * OSB 2.17 service bindings for authenticated platform clients.
 */
@Path("/v2/service_instances/{instance_id}/service_bindings/{binding_id}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceBindingsResource {

    private final PlatformAuthenticator platformAuthenticator;
    private final AdminStore adminStore;
    private final BindingStore bindingStore;
    private final CatalogRepository catalogRepository;

    public ServiceBindingsResource(
            PlatformAuthenticator platformAuthenticator,
            AdminStore adminStore,
            BindingStore bindingStore,
            CatalogRepository catalogRepository) {
        this.platformAuthenticator = platformAuthenticator;
        this.adminStore = adminStore;
        this.bindingStore = bindingStore;
        this.catalogRepository = catalogRepository;
    }

    @PUT
    public Response bind(
            @PathParam("instance_id") String instanceId,
            @PathParam("binding_id") String bindingId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            @HeaderParam("X-Broker-API-Version") String apiVersion,
            @QueryParam("accepts_incomplete") @DefaultValue("false") boolean acceptsIncomplete,
            BindServiceBindingRequest request) {
        PlatformPrincipal principal =
                PlatformBasicAuth.requirePrincipal(platformAuthenticator, authorization);
        ServiceInstanceDto instance = requireOwnedInstance(principal.clientId(), instanceId);

        if (request == null
                || request.serviceId() == null
                || request.serviceId().isBlank()
                || request.planId() == null
                || request.planId().isBlank()) {
            throw new IllegalArgumentException("service_id and plan_id are required");
        }

        ServiceOffering offering = requireOffering(principal.catalogId(), request.serviceId());
        ServicePlan plan = requirePlan(offering, request.planId());
        if (!offering.bindable() || !plan.bindable()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "error", "RequiresApp",
                            "description", "This service plan is not bindable."))
                    .build();
        }
        if (!Objects.equals(instance.serviceId(), request.serviceId())
                || !Objects.equals(instance.planId(), request.planId())) {
            throw new IllegalArgumentException("service_id/plan_id must match the instance");
        }

        var existing = bindingStore.find(instanceId, bindingId);
        if (existing.isPresent() && !isFullyUnbound(existing.get())) {
            ServiceBindingDto current = existing.get();
            if (Objects.equals(current.serviceId(), request.serviceId())
                    && Objects.equals(current.planId(), request.planId())) {
                return Response.ok(toBindingResponse(current)).build();
            }
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("description", "binding already exists with different service/plan"))
                    .build();
        }

        if (!acceptsIncomplete) {
            return Response.status(422)
                    .entity(Map.of(
                            "error", "AsyncRequired",
                            "description",
                            "This service plan requires client support for asynchronous operations."))
                    .build();
        }

        Map<String, Object> parameters =
                PlanParameterSchemas.validateAndApplyDefaults(plan.schemas(), request.parameters());
        ServiceBindingDto created = bindingStore.bind(
                instanceId,
                bindingId,
                request.serviceId(),
                request.planId(),
                principal.clientId(),
                parameters);
        return Response.accepted(toBindingResponse(created)).build();
    }

    @GET
    public Response getBinding(
            @PathParam("instance_id") String instanceId,
            @PathParam("binding_id") String bindingId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            @HeaderParam("X-Broker-API-Version") String apiVersion) {
        PlatformPrincipal principal =
                PlatformBasicAuth.requirePrincipal(platformAuthenticator, authorization);
        requireOwnedInstance(principal.clientId(), instanceId);
        ServiceBindingDto binding = bindingStore
                .find(instanceId, bindingId)
                .filter(b -> !isFullyUnbound(b))
                .orElseThrow(NotFoundException::new);
        return Response.ok(toBindingResponse(binding)).build();
    }

    @DELETE
    public Response unbind(
            @PathParam("instance_id") String instanceId,
            @PathParam("binding_id") String bindingId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            @HeaderParam("X-Broker-API-Version") String apiVersion,
            @QueryParam("service_id") String serviceId,
            @QueryParam("plan_id") String planId,
            @QueryParam("accepts_incomplete") @DefaultValue("false") boolean acceptsIncomplete) {
        PlatformPrincipal principal =
                PlatformBasicAuth.requirePrincipal(platformAuthenticator, authorization);
        requireOwnedInstance(principal.clientId(), instanceId);
        var existing = bindingStore.find(instanceId, bindingId);
        if (existing.isEmpty() || isFullyUnbound(existing.get())) {
            return Response.status(Response.Status.GONE).build();
        }
        if (!acceptsIncomplete) {
            return Response.status(422)
                    .entity(Map.of(
                            "error", "AsyncRequired",
                            "description",
                            "This service plan requires client support for asynchronous operations."))
                    .build();
        }
        ServiceBindingDto deleted =
                bindingStore.unbind(instanceId, bindingId).orElseThrow(NotFoundException::new);
        Map<String, Object> body = new LinkedHashMap<>();
        if (deleted.lastOperationId() != null) {
            body.put("operation", deleted.lastOperationId());
        }
        return Response.accepted(body).build();
    }

    @GET
    @Path("/last_operation")
    public LastOperationResponse lastOperation(
            @PathParam("instance_id") String instanceId,
            @PathParam("binding_id") String bindingId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            @HeaderParam("X-Broker-API-Version") String apiVersion,
            @QueryParam("service_id") String serviceId,
            @QueryParam("plan_id") String planId,
            @QueryParam("operation") String operation) {
        PlatformPrincipal principal =
                PlatformBasicAuth.requirePrincipal(platformAuthenticator, authorization);
        requireOwnedInstance(principal.clientId(), instanceId);
        ServiceBindingDto binding =
                bindingStore.find(instanceId, bindingId).orElseThrow(NotFoundException::new);
        return new LastOperationResponse(
                normalizeLastOperationState(binding.lastOperationState()),
                binding.lastOperationDescription());
    }

    private ServiceInstanceDto requireOwnedInstance(String platformClientId, String instanceId) {
        ServiceInstanceDto instance =
                adminStore.findById(instanceId).orElseThrow(NotFoundException::new);
        if (!Objects.equals(platformClientId, instance.platformClientId())) {
            throw new NotFoundException();
        }
        return instance;
    }

    private ServiceOffering requireOffering(String catalogId, String serviceId) {
        Catalog catalog = catalogRepository
                .findCatalog(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("catalog not found: " + catalogId));
        return catalog.offerings().stream()
                .filter(item -> item.id().equals(serviceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "service not in platform catalog: " + serviceId));
    }

    private static ServicePlan requirePlan(ServiceOffering offering, String planId) {
        return offering.plans().stream()
                .filter(plan -> plan.id().equals(planId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("plan not found: " + planId));
    }

    private static Map<String, Object> toBindingResponse(ServiceBindingDto binding) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("credentials", binding.credentials() == null ? Map.of() : binding.credentials());
        if (binding.lastOperationId() != null && !binding.lastOperationId().isBlank()) {
            body.put("operation", binding.lastOperationId());
        }
        return body;
    }

    private static boolean isFullyUnbound(ServiceBindingDto binding) {
        if (binding == null) {
            return false;
        }
        if ("unbound".equalsIgnoreCase(binding.state())) {
            return true;
        }
        return "UNBIND".equals(binding.lastOperationKind())
                && "succeeded".equalsIgnoreCase(binding.lastOperationState())
                && !"failed".equalsIgnoreCase(binding.state());
    }

    private static String normalizeLastOperationState(String state) {
        if (state == null || state.isBlank()) {
            return "in progress";
        }
        String normalized = state.trim().toLowerCase();
        if ("succeeded".equals(normalized) || "failed".equals(normalized)) {
            return normalized;
        }
        return "in progress";
    }
}
