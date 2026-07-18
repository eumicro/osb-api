package io.osb.api.resource.admin;

import io.osb.api.dto.admin.CreateWorkflowRequest;
import io.osb.api.dto.admin.PageDto;
import io.osb.api.dto.admin.Pages;
import io.osb.api.dto.admin.UpdateWorkflowRequest;
import io.osb.api.dto.admin.WorkflowDefinitionDto;
import io.osb.application.workflows.DeleteWorkflowUseCase;
import io.osb.application.workflows.GetWorkflowUseCase;
import io.osb.application.workflows.ListWorkflowsUseCase;
import io.osb.application.workflows.SaveWorkflowUseCase;
import io.osb.domain.workflows.WorkflowClientType;
import io.osb.domain.workflows.WorkflowDefinition;
import io.osb.domain.workflows.WorkflowKind;
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
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Path("/api/admin/workflows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkflowsAdminResource {

    private final ListWorkflowsUseCase listWorkflowsUseCase;
    private final GetWorkflowUseCase getWorkflowUseCase;
    private final SaveWorkflowUseCase saveWorkflowUseCase;
    private final DeleteWorkflowUseCase deleteWorkflowUseCase;

    public WorkflowsAdminResource(
            ListWorkflowsUseCase listWorkflowsUseCase,
            GetWorkflowUseCase getWorkflowUseCase,
            SaveWorkflowUseCase saveWorkflowUseCase,
            DeleteWorkflowUseCase deleteWorkflowUseCase) {
        this.listWorkflowsUseCase = listWorkflowsUseCase;
        this.getWorkflowUseCase = getWorkflowUseCase;
        this.saveWorkflowUseCase = saveWorkflowUseCase;
        this.deleteWorkflowUseCase = deleteWorkflowUseCase;
    }

    @GET
    public PageDto<WorkflowDefinitionDto> list(
            @QueryParam("page") Integer page,
            @QueryParam("pageSize") Integer pageSize) {
        List<WorkflowDefinitionDto> all =
                listWorkflowsUseCase.execute().stream().map(this::toDto).toList();
        return Pages.of(all, page, pageSize);
    }

    @GET
    @Path("/{id}")
    public WorkflowDefinitionDto get(@PathParam("id") String id) {
        return toDto(getWorkflowUseCase.execute(id));
    }

    @POST
    public Response create(CreateWorkflowRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        WorkflowDefinitionDto created = toDto(saveWorkflowUseCase.create(
                request.name(),
                request.description(),
                parseKind(request.kind()),
                request.n8nWebhookPath(),
                request.n8nWorkflowId(),
                request.enabled(),
                parseClients(request.clients()),
                parseIds(request.httpClientIds()),
                parseIds(request.kubernetesClientIds()),
                parseIds(request.gitClientIds()),
                parseIds(request.templateIds())));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public WorkflowDefinitionDto update(@PathParam("id") String id, UpdateWorkflowRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return toDto(saveWorkflowUseCase.update(
                id,
                request.name(),
                request.description(),
                parseKind(request.kind()),
                request.n8nWebhookPath(),
                request.n8nWorkflowId(),
                request.enabled(),
                parseClients(request.clients()),
                parseIds(request.httpClientIds()),
                parseIds(request.kubernetesClientIds()),
                parseIds(request.gitClientIds()),
                parseIds(request.templateIds())));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        deleteWorkflowUseCase.execute(id);
        return Response.noContent().build();
    }

    private WorkflowDefinitionDto toDto(WorkflowDefinition workflow) {
        return new WorkflowDefinitionDto(
                workflow.id(),
                workflow.name(),
                workflow.description(),
                workflow.kind().name(),
                workflow.n8nWebhookPath(),
                workflow.n8nWorkflowId(),
                workflow.enabled(),
                workflow.clients().stream().map(Enum::name).sorted().toList(),
                workflow.httpClientIds().stream().sorted().toList(),
                workflow.kubernetesClientIds().stream().sorted().toList(),
                workflow.gitClientIds().stream().sorted().toList(),
                workflow.templateIds().stream().sorted().toList());
    }

    private static WorkflowKind parseKind(String kind) {
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("kind is required");
        }
        try {
            return WorkflowKind.valueOf(kind.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unknown workflow kind: " + kind);
        }
    }

    private static Set<WorkflowClientType> parseClients(List<String> clients) {
        if (clients == null || clients.isEmpty()) {
            return Set.of();
        }
        EnumSet<WorkflowClientType> parsed = EnumSet.noneOf(WorkflowClientType.class);
        for (String client : clients) {
            if (client == null || client.isBlank()) {
                continue;
            }
            try {
                parsed.add(WorkflowClientType.valueOf(client.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("unknown workflow client: " + client);
            }
        }
        return parsed;
    }

    private static Set<String> parseIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<String> parsed = new LinkedHashSet<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                parsed.add(id.trim());
            }
        }
        return parsed;
    }
}
