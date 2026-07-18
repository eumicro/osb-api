package io.osb.infrastructure.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.osb.domain.httpclients.HttpClientAuthType;
import io.osb.domain.httpclients.HttpClientInstance;
import io.osb.domain.httpclients.HttpClientInstanceRepository;
import io.osb.port.client.ClientCommandResult;
import io.osb.port.http.HttpClientNetworkPort;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
public class NoOpHttpClientNetwork implements HttpClientNetworkPort {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClientInstanceRepository httpClientInstanceRepository;
    private final HttpClient httpClient = HttpClient.newBuilder()
            // n8n (and some reverse proxies) stall with Java's default HTTP/2.
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public NoOpHttpClientNetwork(HttpClientInstanceRepository httpClientInstanceRepository) {
        this.httpClientInstanceRepository = httpClientInstanceRepository;
    }

    @Override
    public String status() {
        long enabled = httpClientInstanceRepository.list().stream()
                .filter(HttpClientInstance::enabled)
                .count();
        return "http-client-ready (" + enabled + " enabled)";
    }

    @Override
    public String postJson(String url, String jsonBody) {
        // n8n webhooks wait for responseNode (provision can exceed a few seconds).
        return send("POST", url, jsonBody == null ? "{}" : jsonBody, null, 120);
    }

    @Override
    public ClientCommandResult invoke(String action, String payloadJson) {
        String normalized = action == null || action.isBlank()
                ? "status"
                : action.trim().toLowerCase(Locale.ROOT);
        if ("status".equals(normalized)) {
            return ClientCommandResult.success(
                    "HTTP",
                    normalized,
                    status(),
                    "{\"adapter\":\"http\",\"ready\":true}");
        }
        if (!"request".equals(normalized)) {
            return ClientCommandResult.failure(
                    "HTTP", normalized, "unsupported http action: " + action);
        }

        String payload = payloadJson == null ? "{}" : payloadJson;
        JsonNode root = readJson(payload);
        String clientId = textOrNull(root, "clientId");
        String method = Optional.ofNullable(textOrNull(root, "method")).orElse("POST");
        String pathOrUrl = Optional.ofNullable(textOrNull(root, "path"))
                .or(() -> Optional.ofNullable(textOrNull(root, "url")))
                .orElse("");
        // Top-level "body" only — never the nested "body" inside templateContent.
        String body = "{}";
        if (root != null && root.has("body") && !root.get("body").isNull()) {
            JsonNode bodyNode = root.get("body");
            body = bodyNode.isTextual() ? bodyNode.asText() : bodyNode.toString();
            if (body == null || body.isBlank()) {
                body = "{}";
            }
        }

        try {
            if (clientId != null && !clientId.isBlank()) {
                HttpClientInstance instance = httpClientInstanceRepository
                        .findById(clientId.trim())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "http client not found: " + clientId));
                if (!instance.enabled()) {
                    return ClientCommandResult.failure(
                            "HTTP", normalized, "http client disabled: " + clientId);
                }
                String url = instance.resolveUrl(pathOrUrl);
                String responseBody = send(
                        method.toUpperCase(Locale.ROOT),
                        url,
                        body,
                        instance,
                        instance.timeoutSeconds());
                return ClientCommandResult.success(
                        "HTTP",
                        normalized,
                        "http " + method + " via " + instance.id() + " completed",
                        "{\"clientId\":\""
                                + escape(instance.id())
                                + "\",\"url\":\""
                                + escape(url)
                                + "\",\"response\":"
                                + toJsonString(responseBody)
                                + "}");
            }

            if (pathOrUrl.isBlank()) {
                return ClientCommandResult.failure(
                        "HTTP", normalized, "payload.clientId or payload.url is required");
            }
            if (!pathOrUrl.startsWith("http://") && !pathOrUrl.startsWith("https://")) {
                return ClientCommandResult.failure(
                        "HTTP",
                        normalized,
                        "absolute url required when clientId is omitted");
            }
            String responseBody = send(method.toUpperCase(Locale.ROOT), pathOrUrl, body, null, 15);
            return ClientCommandResult.success(
                    "HTTP",
                    normalized,
                    "http " + method + " completed",
                    "{\"url\":\"" + escape(pathOrUrl) + "\",\"response\":"
                            + toJsonString(responseBody)
                            + "}");
        } catch (RuntimeException ex) {
            return ClientCommandResult.failure("HTTP", normalized, ex.getMessage());
        }
    }

    private String send(
            String method,
            String url,
            String jsonBody,
            HttpClientInstance instance,
            int timeoutSeconds) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            applyAuth(builder, instance);
            String body = jsonBody == null ? "{}" : jsonBody;
            switch (method.toUpperCase(Locale.ROOT)) {
                case "GET" -> builder.GET();
                case "DELETE" -> builder.DELETE();
                case "PUT" -> builder.PUT(
                        HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
                case "PATCH" -> builder.method(
                        "PATCH",
                        HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
                default -> builder.POST(
                        HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            }
            HttpResponse<String> response =
                    httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new IllegalStateException(
                        "HTTP " + response.statusCode() + " from " + url + ": " + response.body());
            }
            return response.body() == null ? "" : response.body();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "HTTP " + method + " failed for " + url + ": " + e.getMessage(), e);
        }
    }

    private void applyAuth(HttpRequest.Builder builder, HttpClientInstance instance) {
        if (instance == null || instance.authType() == HttpClientAuthType.NONE) {
            return;
        }
        if (instance.authType() == HttpClientAuthType.BEARER) {
            builder.header("Authorization", "Bearer " + instance.secret());
            return;
        }
        if (instance.authType() == HttpClientAuthType.BASIC) {
            // Keycloak Admin: BASIC + well-known → password grant (admin-cli), then Bearer.
            if (instance.wellKnownUrl() != null && !instance.wellKnownUrl().isBlank()) {
                String accessToken = fetchPasswordGrantToken(instance);
                builder.header("Authorization", "Bearer " + accessToken);
                return;
            }
            String token = Base64.getEncoder()
                    .encodeToString((instance.username() + ":" + instance.secret())
                            .getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + token);
            return;
        }
        if (instance.authType() == HttpClientAuthType.CLIENT_CREDENTIALS) {
            String accessToken = fetchClientCredentialsToken(instance);
            builder.header("Authorization", "Bearer " + accessToken);
        }
    }

    /**
     * Password grant via well-known token endpoint (used for Keycloak admin-cli).
     */
    private String fetchPasswordGrantToken(HttpClientInstance instance) {
        try {
            String tokenEndpoint = resolveTokenEndpoint(instance);
            String clientId = instance.oauthClientId() == null || instance.oauthClientId().isBlank()
                    ? "admin-cli"
                    : instance.oauthClientId();
            String form = "grant_type=password"
                    + "&client_id=" + urlEncode(clientId)
                    + "&username=" + urlEncode(instance.username())
                    + "&password=" + urlEncode(instance.secret());
            HttpRequest tokenRequest = HttpRequest.newBuilder(URI.create(tokenEndpoint))
                    .timeout(Duration.ofSeconds(instance.timeoutSeconds()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> tokenResponse = httpClient.send(
                    tokenRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (tokenResponse.statusCode() >= 400) {
                throw new IllegalStateException(
                        "password-grant HTTP " + tokenResponse.statusCode() + ": "
                                + tokenResponse.body());
            }
            String accessToken = readStringField(tokenResponse.body(), "access_token");
            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalStateException("access_token missing in password-grant response");
            }
            return accessToken;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "password-grant token request failed: " + e.getMessage(), e);
        }
    }

    private String resolveTokenEndpoint(HttpClientInstance instance) throws Exception {
        String wellKnown = instance.wellKnownUrl();
        HttpRequest wellKnownRequest = HttpRequest.newBuilder(URI.create(wellKnown))
                .timeout(Duration.ofSeconds(instance.timeoutSeconds()))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> wellKnownResponse = httpClient.send(
                wellKnownRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (wellKnownResponse.statusCode() >= 400) {
            throw new IllegalStateException(
                    "well-known HTTP " + wellKnownResponse.statusCode() + " from " + wellKnown);
        }
        String tokenEndpoint = readStringField(wellKnownResponse.body(), "token_endpoint");
        if (tokenEndpoint == null || tokenEndpoint.isBlank()) {
            throw new IllegalStateException("token_endpoint missing in well-known document");
        }
        return tokenEndpoint;
    }

    /**
     * Resolves {@code token_endpoint} from OpenID well-known and requests a client-credentials token.
     */
    private String fetchClientCredentialsToken(HttpClientInstance instance) {
        try {
            String tokenEndpoint = resolveTokenEndpoint(instance);
            String form = "grant_type=client_credentials"
                    + "&client_id=" + urlEncode(instance.oauthClientId())
                    + "&client_secret=" + urlEncode(instance.oauthClientSecret());
            HttpRequest tokenRequest = HttpRequest.newBuilder(URI.create(tokenEndpoint))
                    .timeout(Duration.ofSeconds(instance.timeoutSeconds()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> tokenResponse = httpClient.send(
                    tokenRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (tokenResponse.statusCode() >= 400) {
                throw new IllegalStateException(
                        "token HTTP " + tokenResponse.statusCode() + " from " + tokenEndpoint
                                + ": " + tokenResponse.body());
            }
            String accessToken = readStringField(tokenResponse.body(), "access_token");
            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalStateException("access_token missing in token response");
            }
            return accessToken;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "CLIENT_CREDENTIALS token request failed: " + e.getMessage(), e);
        }
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static JsonNode readJson(String json) {
        try {
            return JSON.readTree(json == null || json.isBlank() ? "{}" : json);
        } catch (Exception ex) {
            throw new IllegalStateException("invalid http client payload JSON: " + ex.getMessage(), ex);
        }
    }

    private static String textOrNull(JsonNode root, String field) {
        if (root == null || !root.has(field) || root.get(field).isNull()) {
            return null;
        }
        JsonNode node = root.get(field);
        return node.isTextual() || node.isNumber() || node.isBoolean() ? node.asText() : node.toString();
    }

    private static String readStringField(String json, String field) {
        return textOrNull(readJson(json), field);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String toJsonString(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + escape(value).replace("\n", "\\n") + "\"";
    }
}
