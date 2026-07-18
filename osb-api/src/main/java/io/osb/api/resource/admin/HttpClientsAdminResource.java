package io.osb.api.resource.admin;

import io.osb.api.dto.admin.CreateHttpClientInstanceRequest;
import io.osb.api.dto.admin.HttpClientInstanceDto;
import io.osb.api.dto.admin.PageDto;
import io.osb.api.dto.admin.Pages;
import io.osb.api.dto.admin.UpdateHttpClientInstanceRequest;
import io.osb.application.httpclients.DeleteHttpClientInstanceUseCase;
import io.osb.application.httpclients.GetHttpClientInstanceUseCase;
import io.osb.application.httpclients.ListHttpClientInstancesUseCase;
import io.osb.application.httpclients.SaveHttpClientInstanceUseCase;
import io.osb.domain.httpclients.HttpClientAuthType;
import io.osb.domain.httpclients.HttpClientInstance;
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
import java.util.Locale;

@Path("/api/admin/http-clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HttpClientsAdminResource {

    private final ListHttpClientInstancesUseCase listHttpClientInstancesUseCase;
    private final GetHttpClientInstanceUseCase getHttpClientInstanceUseCase;
    private final SaveHttpClientInstanceUseCase saveHttpClientInstanceUseCase;
    private final DeleteHttpClientInstanceUseCase deleteHttpClientInstanceUseCase;

    public HttpClientsAdminResource(
            ListHttpClientInstancesUseCase listHttpClientInstancesUseCase,
            GetHttpClientInstanceUseCase getHttpClientInstanceUseCase,
            SaveHttpClientInstanceUseCase saveHttpClientInstanceUseCase,
            DeleteHttpClientInstanceUseCase deleteHttpClientInstanceUseCase) {
        this.listHttpClientInstancesUseCase = listHttpClientInstancesUseCase;
        this.getHttpClientInstanceUseCase = getHttpClientInstanceUseCase;
        this.saveHttpClientInstanceUseCase = saveHttpClientInstanceUseCase;
        this.deleteHttpClientInstanceUseCase = deleteHttpClientInstanceUseCase;
    }

    @GET
    public PageDto<HttpClientInstanceDto> list(
            @QueryParam("page") Integer page,
            @QueryParam("pageSize") Integer pageSize) {
        List<HttpClientInstanceDto> all =
                listHttpClientInstancesUseCase.execute().stream().map(this::toDto).toList();
        return Pages.of(all, page, pageSize);
    }

    @GET
    @Path("/{id}")
    public HttpClientInstanceDto get(@PathParam("id") String id) {
        return toDto(getHttpClientInstanceUseCase.execute(id));
    }

    @POST
    public Response create(CreateHttpClientInstanceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        HttpClientInstanceDto created = toDto(saveHttpClientInstanceUseCase.create(
                request.name(),
                request.description(),
                request.baseUrl(),
                parseAuthType(request.authType()),
                request.username(),
                request.secret(),
                request.oauthClientId(),
                request.oauthClientSecret(),
                request.wellKnownUrl(),
                request.timeoutSeconds() == null ? 15 : request.timeoutSeconds(),
                request.enabled() == null || request.enabled()));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public HttpClientInstanceDto update(
            @PathParam("id") String id, UpdateHttpClientInstanceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        boolean keepSecret = request.secret() == null || request.secret().isBlank();
        boolean keepOauthSecret =
                request.oauthClientSecret() == null || request.oauthClientSecret().isBlank();
        return toDto(saveHttpClientInstanceUseCase.update(
                id,
                request.name(),
                request.description(),
                request.baseUrl(),
                parseAuthType(request.authType()),
                request.username(),
                request.secret(),
                keepSecret,
                request.oauthClientId(),
                request.oauthClientSecret(),
                keepOauthSecret,
                request.wellKnownUrl(),
                request.timeoutSeconds() == null ? 15 : request.timeoutSeconds(),
                request.enabled() == null || request.enabled()));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        deleteHttpClientInstanceUseCase.execute(id);
        return Response.noContent().build();
    }

    private HttpClientInstanceDto toDto(HttpClientInstance instance) {
        return new HttpClientInstanceDto(
                instance.id(),
                instance.name(),
                instance.description(),
                instance.baseUrl(),
                instance.authType().name(),
                instance.username(),
                instance.hasSecret(),
                instance.oauthClientId(),
                instance.hasOauthClientSecret(),
                instance.wellKnownUrl(),
                instance.timeoutSeconds(),
                instance.enabled());
    }

    private static HttpClientAuthType parseAuthType(String authType) {
        if (authType == null || authType.isBlank()) {
            return HttpClientAuthType.NONE;
        }
        try {
            return HttpClientAuthType.valueOf(authType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unknown auth type: " + authType);
        }
    }
}
