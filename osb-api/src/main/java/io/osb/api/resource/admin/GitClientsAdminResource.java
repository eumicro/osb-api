package io.osb.api.resource.admin;

import io.osb.api.dto.admin.CreateGitClientInstanceRequest;
import io.osb.api.dto.admin.GitClientInstanceDto;
import io.osb.api.dto.admin.PageDto;
import io.osb.api.dto.admin.Pages;
import io.osb.api.dto.admin.UpdateGitClientInstanceRequest;
import io.osb.application.gitclients.DeleteGitClientInstanceUseCase;
import io.osb.application.gitclients.GetGitClientInstanceUseCase;
import io.osb.application.gitclients.ListGitClientInstancesUseCase;
import io.osb.application.gitclients.SaveGitClientInstanceUseCase;
import io.osb.domain.gitclients.GitClientAuthMethod;
import io.osb.domain.gitclients.GitClientInstance;
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

@Path("/api/admin/git-clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GitClientsAdminResource {

    private final ListGitClientInstancesUseCase listGitClientInstancesUseCase;
    private final GetGitClientInstanceUseCase getGitClientInstanceUseCase;
    private final SaveGitClientInstanceUseCase saveGitClientInstanceUseCase;
    private final DeleteGitClientInstanceUseCase deleteGitClientInstanceUseCase;

    public GitClientsAdminResource(
            ListGitClientInstancesUseCase listGitClientInstancesUseCase,
            GetGitClientInstanceUseCase getGitClientInstanceUseCase,
            SaveGitClientInstanceUseCase saveGitClientInstanceUseCase,
            DeleteGitClientInstanceUseCase deleteGitClientInstanceUseCase) {
        this.listGitClientInstancesUseCase = listGitClientInstancesUseCase;
        this.getGitClientInstanceUseCase = getGitClientInstanceUseCase;
        this.saveGitClientInstanceUseCase = saveGitClientInstanceUseCase;
        this.deleteGitClientInstanceUseCase = deleteGitClientInstanceUseCase;
    }

    @GET
    public PageDto<GitClientInstanceDto> list(
            @QueryParam("page") Integer page,
            @QueryParam("pageSize") Integer pageSize) {
        List<GitClientInstanceDto> all =
                listGitClientInstancesUseCase.execute().stream().map(this::toDto).toList();
        return Pages.of(all, page, pageSize);
    }

    @GET
    @Path("/{id}")
    public GitClientInstanceDto get(@PathParam("id") String id) {
        return toDto(getGitClientInstanceUseCase.execute(id));
    }

    @POST
    public Response create(CreateGitClientInstanceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        GitClientInstanceDto created = toDto(saveGitClientInstanceUseCase.create(
                request.name(),
                request.description(),
                request.remoteUrl(),
                request.defaultBranch() == null || request.defaultBranch().isBlank()
                        ? "main"
                        : request.defaultBranch(),
                parseAuthMethod(request.authMethod()),
                request.username(),
                request.secret(),
                request.passphrase(),
                request.enabled() == null || request.enabled()));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public GitClientInstanceDto update(
            @PathParam("id") String id, UpdateGitClientInstanceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        boolean keepSecret = request.secret() == null || request.secret().isBlank();
        // null passphrase means keep; empty string clears passphrase
        boolean keepPassphrase = request.passphrase() == null;
        return toDto(saveGitClientInstanceUseCase.update(
                id,
                request.name(),
                request.description(),
                request.remoteUrl(),
                request.defaultBranch() == null || request.defaultBranch().isBlank()
                        ? "main"
                        : request.defaultBranch(),
                parseAuthMethod(request.authMethod()),
                request.username(),
                request.secret(),
                keepSecret,
                request.passphrase(),
                keepPassphrase,
                request.enabled() == null || request.enabled()));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        deleteGitClientInstanceUseCase.execute(id);
        return Response.noContent().build();
    }

    private GitClientInstanceDto toDto(GitClientInstance instance) {
        return new GitClientInstanceDto(
                instance.id(),
                instance.name(),
                instance.description(),
                instance.remoteUrl(),
                instance.defaultBranch(),
                instance.authMethod().name(),
                instance.username(),
                instance.hasSecret(),
                instance.hasPassphrase(),
                instance.enabled());
    }

    private static GitClientAuthMethod parseAuthMethod(String authMethod) {
        if (authMethod == null || authMethod.isBlank()) {
            throw new IllegalArgumentException("authMethod is required (HTTPS or SSH)");
        }
        try {
            return GitClientAuthMethod.valueOf(authMethod.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unknown auth method: " + authMethod);
        }
    }
}
