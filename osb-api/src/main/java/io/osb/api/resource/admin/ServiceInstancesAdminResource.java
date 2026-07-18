package io.osb.api.resource.admin;

import io.osb.api.admin.AdminStore;
import io.osb.api.dto.admin.PageDto;
import io.osb.api.dto.admin.Pages;
import io.osb.api.dto.admin.ProvisionInstanceRequest;
import io.osb.api.dto.admin.ServiceInstanceDto;
import io.osb.api.dto.admin.UpdateInstanceRequest;
import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.catalog.PlanParameterSchemas;
import io.osb.domain.catalog.ServiceOffering;
import io.osb.domain.catalog.ServicePlan;
import io.osb.domain.platforms.PlatformClientRepository;
import java.util.Map;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/admin/service-instances")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceInstancesAdminResource {

    private final AdminStore adminStore;
    private final CatalogRepository catalogRepository;
    private final PlatformClientRepository platformClientRepository;

    public ServiceInstancesAdminResource(
            AdminStore adminStore,
            CatalogRepository catalogRepository,
            PlatformClientRepository platformClientRepository) {
        this.adminStore = adminStore;
        this.catalogRepository = catalogRepository;
        this.platformClientRepository = platformClientRepository;
    }

    @GET
    public PageDto<ServiceInstanceDto> list(
            @QueryParam("page") Integer page,
            @QueryParam("pageSize") Integer pageSize) {
        List<ServiceInstanceDto> all = adminStore.listInstances();
        return Pages.of(all, page, pageSize);
    }

    @GET
    @Path("/{id}")
    public ServiceInstanceDto get(@PathParam("id") String id) {
        return adminStore.findById(id).orElseThrow(NotFoundException::new);
    }

    @POST
    public Response provision(ProvisionInstanceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        ServicePlan plan = requireServicePlan(request.serviceId(), request.planId());
        String platformClientId = blankToNull(request.platformClientId());
        if (platformClientId != null) {
            requirePlatform(platformClientId);
        }
        Map<String, Object> parameters =
                PlanParameterSchemas.validateAndApplyDefaults(plan.schemas(), request.parameters());
        ServiceInstanceDto created = adminStore.provision(
                request.serviceId(),
                request.planId(),
                platformClientId,
                parameters);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public ServiceInstanceDto update(@PathParam("id") String id, UpdateInstanceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        adminStore.findById(id).orElseThrow(NotFoundException::new);
        String platformClientId = blankToNull(request.platformClientId());
        if (platformClientId != null) {
            requirePlatform(platformClientId);
        }
        return adminStore.update(id, platformClientId, blankToNull(request.dashboardUrl()));
    }

    @DELETE
    @Path("/{id}")
    public ServiceInstanceDto deprovision(@PathParam("id") String id) {
        return adminStore.deprovision(id).orElseThrow(NotFoundException::new);
    }

    private ServicePlan requireServicePlan(String serviceId, String planId) {
        if (serviceId == null || serviceId.isBlank() || planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("serviceId and planId are required");
        }
        ServiceOffering offering = catalogRepository.listCatalogs().stream()
                .flatMap(catalog -> catalog.offerings().stream())
                .filter(item -> item.id().equals(serviceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("service not found: " + serviceId));
        return offering.plans().stream()
                .filter(plan -> plan.id().equals(planId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("plan not found: " + planId));
    }

    private void requirePlatform(String platformClientId) {
        platformClientRepository
                .findById(platformClientId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "platform client not found: " + platformClientId));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
