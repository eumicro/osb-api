package io.osb.api.resource.osb;

import io.osb.api.admin.AdminStore;
import io.osb.api.dto.admin.ServiceInstanceDto;
import io.osb.api.dto.osb.LastOperationResponse;
import io.osb.api.dto.osb.ProvisionServiceInstanceRequest;
import io.osb.api.dto.osb.ServiceInstanceResponse;
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
import java.util.Map;
import java.util.Objects;

/**
 * OSB 2.17 service instance lifecycle for authenticated platform clients.
 */
@Path("/v2/service_instances/{instance_id}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceInstancesResource {

    private final PlatformAuthenticator platformAuthenticator;
    private final AdminStore adminStore;
    private final CatalogRepository catalogRepository;

    public ServiceInstancesResource(
            PlatformAuthenticator platformAuthenticator,
            AdminStore adminStore,
            CatalogRepository catalogRepository) {
        this.platformAuthenticator = platformAuthenticator;
        this.adminStore = adminStore;
        this.catalogRepository = catalogRepository;
    }

    @PUT
    public Response provision(
            @PathParam("instance_id") String instanceId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            @HeaderParam("X-Broker-API-Version") String apiVersion,
            @QueryParam("accepts_incomplete") @DefaultValue("false") boolean acceptsIncomplete,
            ProvisionServiceInstanceRequest request) {
        PlatformPrincipal principal =
                PlatformBasicAuth.requirePrincipal(platformAuthenticator, authorization);
        if (request == null
                || request.serviceId() == null
                || request.serviceId().isBlank()
                || request.planId() == null
                || request.planId().isBlank()) {
            throw new IllegalArgumentException("service_id and plan_id are required");
        }
        ServicePlan plan = requirePlanInPlatformCatalog(
                principal.catalogId(), request.serviceId(), request.planId());

        var existing = adminStore.findById(instanceId);
        if (existing.isPresent()) {
            ServiceInstanceDto current = existing.get();
            if (!Objects.equals(principal.clientId(), current.platformClientId())) {
                throw new NotFoundException();
            }
            if (Objects.equals(current.serviceId(), request.serviceId())
                    && Objects.equals(current.planId(), request.planId())) {
                return Response.ok(toResponse(current)).build();
            }
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("description", "instance already exists with different service/plan"))
                    .build();
        }

        if (!acceptsIncomplete) {
            return Response.status(422)
                    .entity(Map.of(
                            "error", "AsyncRequired",
                            "description", "This service plan requires client support for asynchronous operations."))
                    .build();
        }

        Map<String, Object> parameters =
                PlanParameterSchemas.validateAndApplyDefaults(plan.schemas(), request.parameters());
        ServiceInstanceDto created = adminStore.provision(
                instanceId,
                request.serviceId(),
                request.planId(),
                principal.clientId(),
                parameters);
        return Response.accepted(toResponse(created)).build();
    }

    @GET
    public Response getInstance(
            @PathParam("instance_id") String instanceId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            @HeaderParam("X-Broker-API-Version") String apiVersion) {
        PlatformPrincipal principal =
                PlatformBasicAuth.requirePrincipal(platformAuthenticator, authorization);
        ServiceInstanceDto instance = requireOwnedInstance(principal.clientId(), instanceId);
        return Response.ok(Map.of(
                        "service_id", instance.serviceId(),
                        "plan_id", instance.planId(),
                        "dashboard_url",
                        instance.dashboardUrl() == null ? "" : instance.dashboardUrl(),
                        "parameters",
                        instance.parameters() == null ? Map.of() : instance.parameters()))
                .build();
    }

    @DELETE
    public Response deprovision(
            @PathParam("instance_id") String instanceId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            @HeaderParam("X-Broker-API-Version") String apiVersion,
            @QueryParam("service_id") String serviceId,
            @QueryParam("plan_id") String planId,
            @QueryParam("accepts_incomplete") @DefaultValue("false") boolean acceptsIncomplete) {
        PlatformPrincipal principal =
                PlatformBasicAuth.requirePrincipal(platformAuthenticator, authorization);
        var existing = adminStore.findById(instanceId);
        if (existing.isEmpty()) {
            return Response.status(Response.Status.GONE).build();
        }
        ServiceInstanceDto current = existing.get();
        if (!Objects.equals(principal.clientId(), current.platformClientId())) {
            throw new NotFoundException();
        }
        if (!acceptsIncomplete) {
            return Response.status(422)
                    .entity(Map.of(
                            "error", "AsyncRequired",
                            "description", "This service plan requires client support for asynchronous operations."))
                    .build();
        }
        ServiceInstanceDto deleted =
                adminStore.deprovision(instanceId).orElseThrow(NotFoundException::new);
        return Response.accepted(toResponse(deleted)).build();
    }

    @GET
    @Path("/last_operation")
    public LastOperationResponse lastOperation(
            @PathParam("instance_id") String instanceId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            @HeaderParam("X-Broker-API-Version") String apiVersion,
            @QueryParam("service_id") String serviceId,
            @QueryParam("plan_id") String planId,
            @QueryParam("operation") String operation) {
        PlatformPrincipal principal =
                PlatformBasicAuth.requirePrincipal(platformAuthenticator, authorization);
        ServiceInstanceDto instance = requireOwnedInstance(principal.clientId(), instanceId);
        String state = normalizeLastOperationState(instance.lastOperationState());
        String description = instance.lastOperationDescription();
        return new LastOperationResponse(state, description);
    }

    private ServiceInstanceDto requireOwnedInstance(String platformClientId, String instanceId) {
        ServiceInstanceDto instance =
                adminStore.findById(instanceId).orElseThrow(NotFoundException::new);
        if (!Objects.equals(platformClientId, instance.platformClientId())) {
            throw new NotFoundException();
        }
        return instance;
    }

    private ServicePlan requirePlanInPlatformCatalog(
            String catalogId, String serviceId, String planId) {
        Catalog catalog = catalogRepository
                .findCatalog(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("catalog not found: " + catalogId));
        ServiceOffering offering = catalog.offerings().stream()
                .filter(item -> item.id().equals(serviceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "service not in platform catalog: " + serviceId));
        return offering.plans().stream()
                .filter(plan -> plan.id().equals(planId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("plan not found: " + planId));
    }

    private static ServiceInstanceResponse toResponse(ServiceInstanceDto instance) {
        String operation = instance.lastOperationId();
        String dashboard = blankToNull(instance.dashboardUrl());
        return new ServiceInstanceResponse(dashboard, operation);
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

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
