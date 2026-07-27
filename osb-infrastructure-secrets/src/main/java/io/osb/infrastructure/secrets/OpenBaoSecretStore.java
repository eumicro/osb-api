package io.osb.infrastructure.secrets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.osb.domain.secrets.SecretStore;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * OpenBao / Vault KV secrets engine v2 adapter (HTTP API).
 */
public final class OpenBaoSecretStore implements SecretStore {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String baseUrl;
    private final String token;
    private final String mount;
    private final HttpClient httpClient;

    public OpenBaoSecretStore(String baseUrl, String token, String mount) {
        this.baseUrl = trimSlash(Objects.requireNonNull(baseUrl, "baseUrl"));
        this.token = Objects.requireNonNull(token, "token");
        if (this.token.isBlank()) {
            throw new IllegalArgumentException("OpenBao token must not be blank");
        }
        String m = Objects.requireNonNullElse(mount, "secret").trim();
        this.mount = m.isBlank() ? "secret" : m;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public void put(String ref, String value) {
        requireRef(ref);
        Objects.requireNonNull(value, "value");
        try {
            ObjectNode body = JSON.createObjectNode();
            ObjectNode data = body.putObject("data");
            data.put("value", value);
            HttpRequest request = HttpRequest.newBuilder(URI.create(dataUrl(ref)))
                    .timeout(Duration.ofSeconds(15))
                    .header("X-Vault-Token", token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new IllegalStateException(
                        "OpenBao put failed (" + response.statusCode() + "): " + response.body());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("OpenBao put failed for " + ref + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<String> get(String ref) {
        requireRef(ref);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(dataUrl(ref)))
                    .timeout(Duration.ofSeconds(15))
                    .header("X-Vault-Token", token)
                    .GET()
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() >= 400) {
                throw new IllegalStateException(
                        "OpenBao get failed (" + response.statusCode() + "): " + response.body());
            }
            JsonNode root = JSON.readTree(response.body());
            JsonNode value = root.path("data").path("data").path("value");
            if (value.isMissingNode() || value.isNull()) {
                return Optional.empty();
            }
            return Optional.of(value.asText());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("OpenBao get failed for " + ref + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String ref) {
        if (ref == null || ref.isBlank()) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(metadataUrl(ref)))
                    .timeout(Duration.ofSeconds(15))
                    .header("X-Vault-Token", token)
                    .DELETE()
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 404) {
                return;
            }
            if (response.statusCode() >= 400) {
                throw new IllegalStateException(
                        "OpenBao delete failed (" + response.statusCode() + "): " + response.body());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "OpenBao delete failed for " + ref + ": " + e.getMessage(), e);
        }
    }

    private String dataUrl(String ref) {
        return baseUrl + "/v1/" + mount + "/data/" + stripLeadingSlash(ref);
    }

    private String metadataUrl(String ref) {
        return baseUrl + "/v1/" + mount + "/metadata/" + stripLeadingSlash(ref);
    }

    private static void requireRef(String ref) {
        if (ref == null || ref.isBlank()) {
            throw new IllegalArgumentException("ref must not be blank");
        }
    }

    private static String trimSlash(String url) {
        String value = url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String stripLeadingSlash(String path) {
        String value = path.trim();
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        return value;
    }
}
