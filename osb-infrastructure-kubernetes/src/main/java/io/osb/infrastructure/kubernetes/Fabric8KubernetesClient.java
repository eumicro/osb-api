package io.osb.infrastructure.kubernetes;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.osb.domain.kubernetesclients.KubernetesClientAuthType;
import io.osb.domain.kubernetesclients.KubernetesClientInstance;
import io.osb.domain.kubernetesclients.KubernetesClientInstanceRepository;
import io.osb.port.client.ClientCommandResult;
import io.osb.port.kubernetes.KubernetesClientPort;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Applies/deletes Kubernetes manifests against the <em>configured</em> client endpoint
 * ({@link KubernetesClientInstance#apiServerUrl()} + auth). Manifest YAML must be supplied
 * by the workflow (payload {@code manifest} / {@code yaml}) — this adapter does not ship
 * product manifests.
 */
@ApplicationScoped
public class Fabric8KubernetesClient implements KubernetesClientPort {

    private static final Set<String> ACTIONS = Set.of("status", "apply", "delete", "get");

    private final KubernetesClientInstanceRepository kubernetesClientInstanceRepository;

    public Fabric8KubernetesClient(
            KubernetesClientInstanceRepository kubernetesClientInstanceRepository) {
        this.kubernetesClientInstanceRepository = kubernetesClientInstanceRepository;
    }

    @Override
    public String status() {
        long enabled = kubernetesClientInstanceRepository.list().stream()
                .filter(KubernetesClientInstance::enabled)
                .count();
        return "kubernetes-adapter-ready (" + enabled + " enabled)";
    }

    @Override
    public ClientCommandResult invoke(String action, String payloadJson) {
        String normalized = normalize(action);
        if (!ACTIONS.contains(normalized)) {
            return ClientCommandResult.failure(
                    "KUBERNETES", normalized, "unsupported kubernetes action: " + action);
        }
        if ("status".equals(normalized)) {
            return ClientCommandResult.success(
                    "KUBERNETES",
                    normalized,
                    status(),
                    "{\"adapter\":\"fabric8\",\"ready\":true}");
        }

        String payload = payloadJson == null || payloadJson.isBlank() ? "{}" : payloadJson;
        String clientId = readStringField(payload, "clientId");
        try {
            if (clientId == null || clientId.isBlank()) {
                return ClientCommandResult.failure(
                        "KUBERNETES", normalized, "payload.clientId is required");
            }
            KubernetesClientInstance instance = kubernetesClientInstanceRepository
                    .findById(clientId.trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "kubernetes client not found: " + clientId));
            if (!instance.enabled()) {
                return ClientCommandResult.failure(
                        "KUBERNETES", normalized, "kubernetes client disabled: " + clientId);
            }
            String namespace = readStringField(payload, "namespace");
            if (namespace == null || namespace.isBlank()) {
                namespace = instance.defaultNamespace();
            }
            String manifest = readStringField(payload, "manifest");
            if (manifest == null || manifest.isBlank()) {
                manifest = readStringField(payload, "yaml");
            }
            if (manifest == null || manifest.isBlank()) {
                return ClientCommandResult.failure(
                        "KUBERNETES",
                        normalized,
                        "payload.manifest (or yaml) is required — supply YAML from the workflow");
            }

            try (KubernetesClient client = buildClient(instance)) {
                if ("apply".equals(normalized)) {
                    ensureNamespace(client, namespace);
                }
                List<HasMetadata> resources = client
                        .load(new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8)))
                        .items();
                if (resources == null || resources.isEmpty()) {
                    return ClientCommandResult.failure(
                            "KUBERNETES", normalized, "manifest contains no Kubernetes resources");
                }
                return switch (normalized) {
                    case "apply" -> apply(client, namespace, resources, instance);
                    case "delete" -> delete(client, namespace, resources, instance);
                    case "get" -> get(client, namespace, resources, instance);
                    default -> ClientCommandResult.failure(
                            "KUBERNETES", normalized, "unsupported kubernetes action: " + action);
                };
            }
        } catch (RuntimeException ex) {
            return ClientCommandResult.failure(
                    "KUBERNETES",
                    normalized,
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }

    private ClientCommandResult apply(
            KubernetesClient client,
            String namespace,
            List<HasMetadata> resources,
            KubernetesClientInstance instance) {
        client.resourceList(resources).inNamespace(namespace).serverSideApply();
        String names = resourceNames(resources);
        return ClientCommandResult.success(
                "KUBERNETES",
                "apply",
                "applied " + names + " in " + namespace,
                details(instance, namespace, names, "applied"));
    }

    private ClientCommandResult delete(
            KubernetesClient client,
            String namespace,
            List<HasMetadata> resources,
            KubernetesClientInstance instance) {
        List<StatusDetails> deleted =
                client.resourceList(resources).inNamespace(namespace).delete();
        String names = resourceNames(resources);
        int count = deleted == null ? 0 : deleted.size();
        // Instance namespace is created on provision (ensureNamespace); remove it on deprovision.
        boolean namespaceDeleted = deleteNamespace(client, namespace);
        return ClientCommandResult.success(
                "KUBERNETES",
                "delete",
                "deleted "
                        + count
                        + " resource(s) ("
                        + names
                        + ") in "
                        + namespace
                        + (namespaceDeleted ? "; namespace deleted" : ""),
                details(
                        instance,
                        namespace,
                        names,
                        "deleted:" + count + (namespaceDeleted ? ",namespaceDeleted" : "")));
    }

    private static boolean deleteNamespace(KubernetesClient client, String namespace) {
        if (namespace == null || namespace.isBlank() || "default".equals(namespace)) {
            return false;
        }
        if (client.namespaces().withName(namespace).get() == null) {
            return false;
        }
        client.namespaces().withName(namespace).delete();
        return true;
    }

    private ClientCommandResult get(
            KubernetesClient client,
            String namespace,
            List<HasMetadata> resources,
            KubernetesClientInstance instance) {
        StringBuilder found = new StringBuilder();
        for (HasMetadata resource : resources) {
            HasMetadata current = client.resource(resource).inNamespace(namespace).get();
            if (found.length() > 0) {
                found.append(',');
            }
            found.append(resource.getKind())
                    .append('/')
                    .append(resource.getMetadata().getName())
                    .append('=')
                    .append(current != null);
        }
        return ClientCommandResult.success(
                "KUBERNETES",
                "get",
                "get in " + namespace + ": " + found,
                details(instance, namespace, found.toString(), "get"));
    }

    /**
     * Connects to the remote API server configured on the client instance.
     * Does not assume a local kubeconfig or co-located cluster.
     */
    private static KubernetesClient buildClient(KubernetesClientInstance instance) {
        ConfigBuilder builder = new ConfigBuilder()
                .withMasterUrl(instance.apiServerUrl())
                .withNamespace(instance.defaultNamespace())
                .withRequestTimeout(instance.timeoutSeconds() * 1000)
                .withConnectionTimeout(instance.timeoutSeconds() * 1000)
                .withTrustCerts(instance.insecureSkipTlsVerify());

        switch (instance.authType()) {
            case BEARER -> {
                if (!instance.hasToken()) {
                    throw new IllegalArgumentException(
                            "kubernetes client " + instance.id() + " requires a bearer token");
                }
                builder.withOauthToken(instance.token());
            }
            case BASIC -> {
                if (instance.username().isBlank() || !instance.hasToken()) {
                    throw new IllegalArgumentException(
                            "kubernetes client " + instance.id() + " requires BASIC username/password");
                }
                builder.withUsername(instance.username()).withPassword(instance.token());
            }
            case NONE -> {
                // Anonymous / insecure lab clusters only
            }
            case CLIENT_CREDENTIALS -> throw new IllegalArgumentException(
                    "CLIENT_CREDENTIALS auth is not supported for Kubernetes API access");
        }

        Config config = builder.build();
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    private static void ensureNamespace(KubernetesClient client, String namespace) {
        if (client.namespaces().withName(namespace).get() != null) {
            return;
        }
        client.namespaces()
                .resource(new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(namespace)
                        .endMetadata()
                        .build())
                .create();
    }

    private static String resourceNames(List<HasMetadata> resources) {
        return resources.stream()
                .map(r -> r.getKind() + "/" + r.getMetadata().getName())
                .collect(Collectors.joining(","));
    }

    private static String details(
            KubernetesClientInstance instance, String namespace, String resources, String result) {
        return "{\"clientId\":\""
                + escape(instance.id())
                + "\",\"apiServerUrl\":\""
                + escape(instance.apiServerUrl())
                + "\",\"namespace\":\""
                + escape(namespace)
                + "\",\"resources\":\""
                + escape(resources)
                + "\",\"result\":\""
                + escape(result)
                + "\",\"adapter\":\"fabric8\"}";
    }

    private static String normalize(String action) {
        return action == null || action.isBlank()
                ? "status"
                : action.trim().toLowerCase(Locale.ROOT);
    }

    private static String readStringField(String json, String field) {
        String key = "\"" + field + "\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) {
            return null;
        }
        int colon = json.indexOf(':', keyIdx + key.length());
        if (colon < 0) {
            return null;
        }
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length() || json.charAt(i) != '"') {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        i++;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'u' -> {
                        if (i + 5 < json.length()) {
                            String hex = json.substring(i + 2, i + 6);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 6;
                            continue;
                        }
                        sb.append(next);
                    }
                    default -> sb.append(next);
                }
                i += 2;
                continue;
            }
            if (c == '"') {
                break;
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
