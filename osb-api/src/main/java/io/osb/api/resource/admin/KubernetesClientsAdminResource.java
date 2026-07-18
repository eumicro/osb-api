package io.osb.api.resource.admin;

import io.osb.api.dto.admin.CreateKubernetesClientInstanceRequest;
import io.osb.api.dto.admin.KubernetesClientInstanceDto;
import io.osb.api.dto.admin.PageDto;
import io.osb.api.dto.admin.Pages;
import io.osb.api.dto.admin.UpdateKubernetesClientInstanceRequest;
import io.osb.application.kubernetesclients.DeleteKubernetesClientInstanceUseCase;
import io.osb.application.kubernetesclients.GetKubernetesClientInstanceUseCase;
import io.osb.application.kubernetesclients.ListKubernetesClientInstancesUseCase;
import io.osb.application.kubernetesclients.SaveKubernetesClientInstanceUseCase;
import io.osb.domain.kubernetesclients.KubernetesClientAuthType;
import io.osb.domain.kubernetesclients.KubernetesClientInstance;
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

@Path("/api/admin/kubernetes-clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class KubernetesClientsAdminResource {

    private final ListKubernetesClientInstancesUseCase listKubernetesClientInstancesUseCase;
    private final GetKubernetesClientInstanceUseCase getKubernetesClientInstanceUseCase;
    private final SaveKubernetesClientInstanceUseCase saveKubernetesClientInstanceUseCase;
    private final DeleteKubernetesClientInstanceUseCase deleteKubernetesClientInstanceUseCase;

    public KubernetesClientsAdminResource(
            ListKubernetesClientInstancesUseCase listKubernetesClientInstancesUseCase,
            GetKubernetesClientInstanceUseCase getKubernetesClientInstanceUseCase,
            SaveKubernetesClientInstanceUseCase saveKubernetesClientInstanceUseCase,
            DeleteKubernetesClientInstanceUseCase deleteKubernetesClientInstanceUseCase) {
        this.listKubernetesClientInstancesUseCase = listKubernetesClientInstancesUseCase;
        this.getKubernetesClientInstanceUseCase = getKubernetesClientInstanceUseCase;
        this.saveKubernetesClientInstanceUseCase = saveKubernetesClientInstanceUseCase;
        this.deleteKubernetesClientInstanceUseCase = deleteKubernetesClientInstanceUseCase;
    }

    @GET
    public PageDto<KubernetesClientInstanceDto> list(
            @QueryParam("page") Integer page,
            @QueryParam("pageSize") Integer pageSize) {
        List<KubernetesClientInstanceDto> all = listKubernetesClientInstancesUseCase.execute().stream()
                .map(this::toDto)
                .toList();
        return Pages.of(all, page, pageSize);
    }

    @GET
    @Path("/{id}")
    public KubernetesClientInstanceDto get(@PathParam("id") String id) {
        return toDto(getKubernetesClientInstanceUseCase.execute(id));
    }

    @POST
    public Response create(CreateKubernetesClientInstanceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        KubernetesClientInstanceDto created = toDto(saveKubernetesClientInstanceUseCase.create(
                request.name(),
                request.description(),
                request.apiServerUrl(),
                request.defaultNamespace() == null || request.defaultNamespace().isBlank()
                        ? "default"
                        : request.defaultNamespace(),
                parseAuthType(request.authType()),
                request.username(),
                request.token(),
                request.oauthClientId(),
                request.oauthClientSecret(),
                request.wellKnownUrl(),
                request.insecureSkipTlsVerify() != null && request.insecureSkipTlsVerify(),
                request.timeoutSeconds() == null ? 30 : request.timeoutSeconds(),
                request.enabled() == null || request.enabled()));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public KubernetesClientInstanceDto update(
            @PathParam("id") String id, UpdateKubernetesClientInstanceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        boolean keepToken = request.token() == null || request.token().isBlank();
        boolean keepOauthSecret =
                request.oauthClientSecret() == null || request.oauthClientSecret().isBlank();
        return toDto(saveKubernetesClientInstanceUseCase.update(
                id,
                request.name(),
                request.description(),
                request.apiServerUrl(),
                request.defaultNamespace() == null || request.defaultNamespace().isBlank()
                        ? "default"
                        : request.defaultNamespace(),
                parseAuthType(request.authType()),
                request.username(),
                request.token(),
                keepToken,
                request.oauthClientId(),
                request.oauthClientSecret(),
                keepOauthSecret,
                request.wellKnownUrl(),
                request.insecureSkipTlsVerify() != null && request.insecureSkipTlsVerify(),
                request.timeoutSeconds() == null ? 30 : request.timeoutSeconds(),
                request.enabled() == null || request.enabled()));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        deleteKubernetesClientInstanceUseCase.execute(id);
        return Response.noContent().build();
    }

    private KubernetesClientInstanceDto toDto(KubernetesClientInstance instance) {
        return new KubernetesClientInstanceDto(
                instance.id(),
                instance.name(),
                instance.description(),
                instance.apiServerUrl(),
                instance.defaultNamespace(),
                instance.authType().name(),
                instance.username(),
                instance.hasToken(),
                instance.oauthClientId(),
                instance.hasOauthClientSecret(),
                instance.wellKnownUrl(),
                instance.insecureSkipTlsVerify(),
                instance.timeoutSeconds(),
                instance.enabled());
    }

    private static KubernetesClientAuthType parseAuthType(String authType) {
        if (authType == null || authType.isBlank()) {
            return KubernetesClientAuthType.NONE;
        }
        try {
            return KubernetesClientAuthType.valueOf(authType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unknown auth type: " + authType);
        }
    }
}
