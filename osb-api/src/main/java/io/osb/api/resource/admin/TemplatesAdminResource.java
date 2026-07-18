package io.osb.api.resource.admin;

import io.osb.api.dto.admin.CreateTemplateRequest;
import io.osb.api.dto.admin.PageDto;
import io.osb.api.dto.admin.Pages;
import io.osb.api.dto.admin.TemplateDto;
import io.osb.api.dto.admin.UpdateTemplateRequest;
import io.osb.application.templates.DeleteTemplateUseCase;
import io.osb.application.templates.GetTemplateUseCase;
import io.osb.application.templates.ListTemplatesUseCase;
import io.osb.application.templates.SaveTemplateUseCase;
import io.osb.domain.templates.Template;
import io.osb.domain.templates.TemplateKind;
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

@Path("/api/admin/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TemplatesAdminResource {

    private final ListTemplatesUseCase listTemplatesUseCase;
    private final GetTemplateUseCase getTemplateUseCase;
    private final SaveTemplateUseCase saveTemplateUseCase;
    private final DeleteTemplateUseCase deleteTemplateUseCase;

    public TemplatesAdminResource(
            ListTemplatesUseCase listTemplatesUseCase,
            GetTemplateUseCase getTemplateUseCase,
            SaveTemplateUseCase saveTemplateUseCase,
            DeleteTemplateUseCase deleteTemplateUseCase) {
        this.listTemplatesUseCase = listTemplatesUseCase;
        this.getTemplateUseCase = getTemplateUseCase;
        this.saveTemplateUseCase = saveTemplateUseCase;
        this.deleteTemplateUseCase = deleteTemplateUseCase;
    }

    @GET
    public PageDto<TemplateDto> list(
            @QueryParam("page") Integer page, @QueryParam("pageSize") Integer pageSize) {
        List<TemplateDto> all =
                listTemplatesUseCase.execute().stream().map(this::toDto).toList();
        return Pages.of(all, page, pageSize);
    }

    @GET
    @Path("/{id}")
    public TemplateDto get(@PathParam("id") String id) {
        return toDto(getTemplateUseCase.execute(id));
    }

    @POST
    public Response create(CreateTemplateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        TemplateDto created = toDto(saveTemplateUseCase.create(
                request.name(),
                request.description(),
                parseKind(request.kind()),
                request.content() == null ? "" : request.content(),
                request.enabled() == null || request.enabled()));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public TemplateDto update(@PathParam("id") String id, UpdateTemplateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return toDto(saveTemplateUseCase.update(
                id,
                request.name(),
                request.description(),
                parseKind(request.kind()),
                request.content() == null ? "" : request.content(),
                request.enabled() == null || request.enabled()));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        deleteTemplateUseCase.execute(id);
        return Response.noContent().build();
    }

    private TemplateDto toDto(Template template) {
        return new TemplateDto(
                template.id(),
                template.name(),
                template.description(),
                template.kind().name(),
                template.content(),
                template.enabled());
    }

    private static TemplateKind parseKind(String kind) {
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("kind is required");
        }
        try {
            return TemplateKind.valueOf(kind.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unknown template kind: " + kind);
        }
    }
}
