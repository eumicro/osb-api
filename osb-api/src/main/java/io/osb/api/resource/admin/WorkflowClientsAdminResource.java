package io.osb.api.resource.admin;

import io.osb.api.dto.admin.WorkflowClientStatusDto;
import io.osb.domain.workflows.WorkflowClientType;
import io.osb.port.git.GitClientPort;
import io.osb.port.http.HttpClientNetworkPort;
import io.osb.port.kubernetes.KubernetesClientPort;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/admin/workflow-clients")
@Produces(MediaType.APPLICATION_JSON)
public class WorkflowClientsAdminResource {

    private final GitClientPort gitClientPort;
    private final KubernetesClientPort kubernetesClientPort;
    private final HttpClientNetworkPort httpClientNetworkPort;

    public WorkflowClientsAdminResource(
            GitClientPort gitClientPort,
            KubernetesClientPort kubernetesClientPort,
            HttpClientNetworkPort httpClientNetworkPort) {
        this.gitClientPort = gitClientPort;
        this.kubernetesClientPort = kubernetesClientPort;
        this.httpClientNetworkPort = httpClientNetworkPort;
    }

    @GET
    public List<WorkflowClientStatusDto> list() {
        return List.of(
                new WorkflowClientStatusDto(WorkflowClientType.GIT.name(), gitClientPort.status()),
                new WorkflowClientStatusDto(
                        WorkflowClientType.KUBERNETES.name(), kubernetesClientPort.status()),
                new WorkflowClientStatusDto(
                        WorkflowClientType.HTTP.name(), httpClientNetworkPort.status()));
    }
}
