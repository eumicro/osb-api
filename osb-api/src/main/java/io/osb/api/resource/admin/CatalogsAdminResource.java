package io.osb.api.resource.admin;

import io.osb.api.dto.admin.CatalogDto;
import io.osb.api.dto.admin.CreateCatalogRequest;
import io.osb.api.dto.admin.PageDto;
import io.osb.api.dto.admin.Pages;
import io.osb.api.dto.admin.CreateOfferingRequest;
import io.osb.api.dto.admin.MovePlanRequest;
import io.osb.api.dto.admin.SavePlanRequest;
import io.osb.api.dto.admin.UpdateCatalogRequest;
import io.osb.api.dto.admin.UpdateOfferingRequest;
import io.osb.api.dto.osb.ServiceOfferingDto;
import io.osb.api.dto.osb.ServicePlanDto;
import io.osb.api.mapper.CatalogMapper;
import io.osb.application.catalog.DeleteCatalogUseCase;
import io.osb.application.catalog.DeleteOfferingUseCase;
import io.osb.application.catalog.DeletePlanUseCase;
import io.osb.application.catalog.GetCatalogByIdUseCase;
import io.osb.application.catalog.GetOfferingUseCase;
import io.osb.application.catalog.GetPlanUseCase;
import io.osb.application.catalog.ListCatalogsUseCase;
import io.osb.application.catalog.MovePlanUseCase;
import io.osb.application.catalog.SaveCatalogUseCase;
import io.osb.application.catalog.SaveOfferingUseCase;
import io.osb.application.catalog.SavePlanUseCase;
import io.osb.domain.catalog.ServicePlan;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/admin/catalogs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogsAdminResource {

    private final ListCatalogsUseCase listCatalogsUseCase;
    private final GetCatalogByIdUseCase getCatalogByIdUseCase;
    private final SaveCatalogUseCase saveCatalogUseCase;
    private final DeleteCatalogUseCase deleteCatalogUseCase;
    private final GetOfferingUseCase getOfferingUseCase;
    private final SaveOfferingUseCase saveOfferingUseCase;
    private final DeleteOfferingUseCase deleteOfferingUseCase;
    private final GetPlanUseCase getPlanUseCase;
    private final SavePlanUseCase savePlanUseCase;
    private final DeletePlanUseCase deletePlanUseCase;
    private final MovePlanUseCase movePlanUseCase;
    private final CatalogMapper catalogMapper;

    public CatalogsAdminResource(
            ListCatalogsUseCase listCatalogsUseCase,
            GetCatalogByIdUseCase getCatalogByIdUseCase,
            SaveCatalogUseCase saveCatalogUseCase,
            DeleteCatalogUseCase deleteCatalogUseCase,
            GetOfferingUseCase getOfferingUseCase,
            SaveOfferingUseCase saveOfferingUseCase,
            DeleteOfferingUseCase deleteOfferingUseCase,
            GetPlanUseCase getPlanUseCase,
            SavePlanUseCase savePlanUseCase,
            DeletePlanUseCase deletePlanUseCase,
            MovePlanUseCase movePlanUseCase,
            CatalogMapper catalogMapper) {
        this.listCatalogsUseCase = listCatalogsUseCase;
        this.getCatalogByIdUseCase = getCatalogByIdUseCase;
        this.saveCatalogUseCase = saveCatalogUseCase;
        this.deleteCatalogUseCase = deleteCatalogUseCase;
        this.getOfferingUseCase = getOfferingUseCase;
        this.saveOfferingUseCase = saveOfferingUseCase;
        this.deleteOfferingUseCase = deleteOfferingUseCase;
        this.getPlanUseCase = getPlanUseCase;
        this.savePlanUseCase = savePlanUseCase;
        this.deletePlanUseCase = deletePlanUseCase;
        this.movePlanUseCase = movePlanUseCase;
        this.catalogMapper = catalogMapper;
    }

    @GET
    public PageDto<CatalogDto> list(
            @QueryParam("page") Integer page,
            @QueryParam("pageSize") Integer pageSize) {
        List<CatalogDto> all =
                listCatalogsUseCase.execute().stream().map(catalogMapper::toAdminDto).toList();
        return Pages.of(all, page, pageSize);
    }

    @GET
    @Path("/{catalogId}")
    public CatalogDto get(@PathParam("catalogId") String catalogId) {
        return catalogMapper.toAdminDto(getCatalogByIdUseCase.execute(catalogId));
    }

    @POST
    public Response create(CreateCatalogRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        CatalogDto created = catalogMapper.toAdminDto(saveCatalogUseCase.create(
                request.id(),
                request.name(),
                request.description() == null ? "" : request.description()));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{catalogId}")
    public CatalogDto update(@PathParam("catalogId") String catalogId, UpdateCatalogRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return catalogMapper.toAdminDto(saveCatalogUseCase.update(
                catalogId,
                request.name(),
                request.description() == null ? "" : request.description()));
    }

    @DELETE
    @Path("/{catalogId}")
    public Response delete(@PathParam("catalogId") String catalogId) {
        deleteCatalogUseCase.execute(catalogId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{catalogId}/offerings")
    public PageDto<ServiceOfferingDto> listOfferings(
            @PathParam("catalogId") String catalogId,
            @QueryParam("page") Integer page,
            @QueryParam("pageSize") Integer pageSize) {
        List<ServiceOfferingDto> all =
                catalogMapper.toAdminDto(getCatalogByIdUseCase.execute(catalogId)).offerings();
        return Pages.of(all, page, pageSize);
    }

    @GET
    @Path("/{catalogId}/offerings/{offeringId}")
    public ServiceOfferingDto getOffering(
            @PathParam("catalogId") String catalogId,
            @PathParam("offeringId") String offeringId) {
        return catalogMapper.toDto(getOfferingUseCase.execute(catalogId, offeringId));
    }

    @POST
    @Path("/{catalogId}/offerings")
    public Response createOffering(
            @PathParam("catalogId") String catalogId,
            CreateOfferingRequest request) {
        if (request == null || request.initialPlan() == null) {
            throw new IllegalArgumentException("initialPlan is required");
        }
        ServiceOfferingDto created = catalogMapper.toDto(saveOfferingUseCase.create(
                catalogId,
                request.id(),
                request.name(),
                request.description() == null ? "" : request.description(),
                request.bindable(),
                catalogMapper.toDomain(request.initialPlan())));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{catalogId}/offerings/{offeringId}")
    public ServiceOfferingDto updateOffering(
            @PathParam("catalogId") String catalogId,
            @PathParam("offeringId") String offeringId,
            UpdateOfferingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return catalogMapper.toDto(saveOfferingUseCase.update(
                catalogId,
                offeringId,
                request.name(),
                request.description() == null ? "" : request.description(),
                request.bindable()));
    }

    @DELETE
    @Path("/{catalogId}/offerings/{offeringId}")
    public Response deleteOffering(
            @PathParam("catalogId") String catalogId,
            @PathParam("offeringId") String offeringId) {
        deleteOfferingUseCase.execute(catalogId, offeringId);
        return Response.noContent().build();
    }

    @POST
    @Path("/{catalogId}/offerings/{offeringId}/plans")
    public Response createPlan(
            @PathParam("catalogId") String catalogId,
            @PathParam("offeringId") String offeringId,
            SavePlanRequest request) {
        ServiceOfferingDto updated = catalogMapper.toDto(
                savePlanUseCase.create(catalogId, offeringId, toPlan(request)));
        return Response.status(Response.Status.CREATED).entity(updated).build();
    }

    @GET
    @Path("/{catalogId}/offerings/{offeringId}/plans/{planId}")
    public ServicePlanDto getPlan(
            @PathParam("catalogId") String catalogId,
            @PathParam("offeringId") String offeringId,
            @PathParam("planId") String planId) {
        return catalogMapper.toDto(getPlanUseCase.execute(catalogId, offeringId, planId));
    }

    @PUT
    @Path("/{catalogId}/offerings/{offeringId}/plans/{planId}")
    public ServiceOfferingDto updatePlan(
            @PathParam("catalogId") String catalogId,
            @PathParam("offeringId") String offeringId,
            @PathParam("planId") String planId,
            SavePlanRequest request) {
        if (request == null || request.id() == null || !request.id().equals(planId)) {
            throw new IllegalArgumentException("plan id in path and body must match");
        }
        return catalogMapper.toDto(savePlanUseCase.update(catalogId, offeringId, toPlan(request)));
    }

    @DELETE
    @Path("/{catalogId}/offerings/{offeringId}/plans/{planId}")
    public ServiceOfferingDto deletePlan(
            @PathParam("catalogId") String catalogId,
            @PathParam("offeringId") String offeringId,
            @PathParam("planId") String planId) {
        return catalogMapper.toDto(deletePlanUseCase.execute(catalogId, offeringId, planId));
    }

    @POST
    @Path("/{catalogId}/offerings/{offeringId}/plans/{planId}/move")
    public ServiceOfferingDto movePlan(
            @PathParam("catalogId") String catalogId,
            @PathParam("offeringId") String offeringId,
            @PathParam("planId") String planId,
            MovePlanRequest request) {
        if (request == null || request.targetOfferingId() == null || request.targetOfferingId().isBlank()) {
            throw new IllegalArgumentException("targetOfferingId is required");
        }
        return catalogMapper.toDto(movePlanUseCase.execute(
                catalogId, offeringId, planId, request.targetOfferingId()));
    }

    private ServicePlan toPlan(SavePlanRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return catalogMapper.toDomain(new ServicePlanDto(
                request.id(),
                request.name(),
                request.description() == null ? "" : request.description(),
                request.free(),
                request.bindable(),
                request.schemas(),
                request.parametersUiSchema()));
    }
}
