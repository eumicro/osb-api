package io.osb.api.resource.admin;

import io.osb.api.dto.admin.CreatePlatformClientRequest;
import io.osb.api.dto.admin.PageDto;
import io.osb.api.dto.admin.Pages;
import io.osb.api.dto.admin.PlatformClientDto;
import io.osb.api.dto.admin.UpdatePlatformClientRequest;
import io.osb.api.mapper.CatalogMapper;
import io.osb.application.platforms.DeletePlatformClientUseCase;
import io.osb.application.platforms.GetPlatformClientUseCase;
import io.osb.application.platforms.ListPlatformClientsUseCase;
import io.osb.application.platforms.SavePlatformClientUseCase;
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

@Path("/api/admin/platform-clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlatformsAdminResource {

    private final ListPlatformClientsUseCase listPlatformClientsUseCase;
    private final GetPlatformClientUseCase getPlatformClientUseCase;
    private final SavePlatformClientUseCase savePlatformClientUseCase;
    private final DeletePlatformClientUseCase deletePlatformClientUseCase;
    private final CatalogMapper catalogMapper;

    public PlatformsAdminResource(
            ListPlatformClientsUseCase listPlatformClientsUseCase,
            GetPlatformClientUseCase getPlatformClientUseCase,
            SavePlatformClientUseCase savePlatformClientUseCase,
            DeletePlatformClientUseCase deletePlatformClientUseCase,
            CatalogMapper catalogMapper) {
        this.listPlatformClientsUseCase = listPlatformClientsUseCase;
        this.getPlatformClientUseCase = getPlatformClientUseCase;
        this.savePlatformClientUseCase = savePlatformClientUseCase;
        this.deletePlatformClientUseCase = deletePlatformClientUseCase;
        this.catalogMapper = catalogMapper;
    }

    @GET
    public PageDto<PlatformClientDto> list(
            @QueryParam("page") Integer page,
            @QueryParam("pageSize") Integer pageSize) {
        List<PlatformClientDto> all =
                listPlatformClientsUseCase.execute().stream().map(catalogMapper::toDto).toList();
        return Pages.of(all, page, pageSize);
    }

    @GET
    @Path("/{id}")
    public PlatformClientDto get(@PathParam("id") String id) {
        return catalogMapper.toDto(getPlatformClientUseCase.execute(id));
    }

    @POST
    public Response create(CreatePlatformClientRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        PlatformClientDto created = catalogMapper.toDto(savePlatformClientUseCase.create(
                request.displayName(),
                request.username(),
                request.catalogId(),
                request.enabled()));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public PlatformClientDto update(@PathParam("id") String id, UpdatePlatformClientRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return catalogMapper.toDto(savePlatformClientUseCase.update(
                id,
                request.displayName(),
                request.username(),
                request.catalogId(),
                request.enabled()));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        deletePlatformClientUseCase.execute(id);
        return Response.noContent().build();
    }
}
