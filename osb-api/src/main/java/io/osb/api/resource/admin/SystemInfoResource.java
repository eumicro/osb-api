package io.osb.api.resource.admin;

import io.osb.api.config.OsbServicesConfig;
import io.osb.api.dto.admin.SystemInfoResponse;
import io.osb.port.git.GitClientPort;
import io.osb.port.http.HttpClientNetworkPort;
import io.osb.port.kubernetes.KubernetesClientPort;
import io.osb.workflow.DeprovisioningWorkflow;
import io.osb.workflow.ProvisioningWorkflow;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/info")
@Produces(MediaType.APPLICATION_JSON)
public class SystemInfoResource {

    private final GitClientPort gitClientPort;
    private final KubernetesClientPort kubernetesClientPort;
    private final HttpClientNetworkPort httpClientNetworkPort;
    private final ProvisioningWorkflow provisioningWorkflow;
    private final DeprovisioningWorkflow deprovisioningWorkflow;
    private final OsbServicesConfig.Postgres postgres;
    private final OsbServicesConfig.Keycloak keycloak;
    private final String n8nBaseUrl;

    public SystemInfoResource(
            GitClientPort gitClientPort,
            KubernetesClientPort kubernetesClientPort,
            HttpClientNetworkPort httpClientNetworkPort,
            ProvisioningWorkflow provisioningWorkflow,
            DeprovisioningWorkflow deprovisioningWorkflow,
            OsbServicesConfig.Postgres postgres,
            OsbServicesConfig.Keycloak keycloak,
            @ConfigProperty(name = "osb.n8n.base-url") String n8nBaseUrl) {
        this.gitClientPort = gitClientPort;
        this.kubernetesClientPort = kubernetesClientPort;
        this.httpClientNetworkPort = httpClientNetworkPort;
        this.provisioningWorkflow = provisioningWorkflow;
        this.deprovisioningWorkflow = deprovisioningWorkflow;
        this.postgres = postgres;
        this.keycloak = keycloak;
        this.n8nBaseUrl = n8nBaseUrl;
    }

    @GET
    public SystemInfoResponse info() {
        Map<String, String> adapters = new LinkedHashMap<>();
        adapters.put("git", gitClientPort.status());
        adapters.put("kubernetes", kubernetesClientPort.status());
        adapters.put("httpClient", httpClientNetworkPort.status());
        adapters.put("provisioningWorkflow", provisioningWorkflow.getClass().getSimpleName());
        adapters.put("deprovisioningWorkflow", deprovisioningWorkflow.getClass().getSimpleName());

        Map<String, String> services = new LinkedHashMap<>();
        services.put("postgresJdbcUrl", postgres.jdbcUrl());
        services.put("keycloakUrl", keycloak.url());
        services.put("keycloakRealm", keycloak.realm());
        services.put("n8nUrl", n8nBaseUrl);

        return new SystemInfoResponse(
                "OSB-API",
                "0.1.0-SNAPSHOT",
                "skeleton-ready",
                adapters,
                services);
    }
}
