package io.osb.infrastructure.git;

import io.osb.domain.gitclients.GitClientAuthMethod;
import io.osb.domain.gitclients.GitClientInstance;
import io.osb.domain.gitclients.GitClientInstanceRepository;
import io.osb.port.client.ClientCommandResult;
import io.osb.port.git.GitClientPort;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Real Git operations against a Gitea HTTP API (contents upsert/get/delete).
 * Payload fields: {@code clientId}, {@code path}, {@code content}, {@code message}, optional {@code ref}.
 */
@ApplicationScoped
public class GiteaHttpGitClient implements GitClientPort {

    private static final Set<String> ACTIONS =
            Set.of("status", "clone", "commit", "push", "checkout", "delete");
    private static final Pattern REMOTE = Pattern.compile(
            "^(https?://[^/]+)/([^/]+)/([^/]+?)(?:\\.git)?/?$", Pattern.CASE_INSENSITIVE);

    private final GitClientInstanceRepository gitClientInstanceRepository;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public GiteaHttpGitClient(GitClientInstanceRepository gitClientInstanceRepository) {
        this.gitClientInstanceRepository = gitClientInstanceRepository;
    }

    @Override
    public String status() {
        long enabled = gitClientInstanceRepository.list().stream()
                .filter(GitClientInstance::enabled)
                .count();
        return "git-adapter-ready (" + enabled + " enabled)";
    }

    @Override
    public ClientCommandResult invoke(String action, String payloadJson) {
        String normalized = normalize(action);
        if (!ACTIONS.contains(normalized)) {
            return ClientCommandResult.failure(
                    "GIT", normalized, "unsupported git action: " + action);
        }
        if ("status".equals(normalized) && !payloadHasClient(payloadJson)) {
            return ClientCommandResult.success(
                    "GIT", normalized, status(), "{\"adapter\":\"gitea-http\",\"ready\":true}");
        }

        String payload = payloadJson == null || payloadJson.isBlank() ? "{}" : payloadJson;
        String clientId = readStringField(payload, "clientId");
        try {
            if (clientId == null || clientId.isBlank()) {
                return ClientCommandResult.failure(
                        "GIT", normalized, "payload.clientId is required");
            }
            GitClientInstance instance = gitClientInstanceRepository
                    .findById(clientId.trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "git client not found: " + clientId));
            if (!instance.enabled()) {
                return ClientCommandResult.failure(
                        "GIT", normalized, "git client disabled: " + clientId);
            }
            if (instance.authMethod() != GitClientAuthMethod.HTTPS) {
                return ClientCommandResult.failure(
                        "GIT",
                        normalized,
                        "Gitea HTTP adapter requires HTTPS auth (got " + instance.authMethod() + ")");
            }

            GiteaRemote remote = parseRemote(instance.resolveRemote(readStringField(payload, "repository")));
            String branch = firstNonBlank(
                    readStringField(payload, "ref"),
                    readStringField(payload, "branch"),
                    instance.defaultBranch());
            String path = firstNonBlank(
                    readStringField(payload, "path"),
                    readStringField(payload, "filePath"));
            String content = firstNonBlank(
                    readStringField(payload, "content"),
                    readStringField(payload, "commandLine"));
            String message = firstNonBlank(
                    readStringField(payload, "message"),
                    "OSB " + normalized);

            return switch (normalized) {
                case "clone", "checkout" -> getFile(instance, remote, path, branch, normalized);
                case "status" -> getFile(instance, remote, path, branch, normalized);
                case "commit", "push" -> upsertFile(
                        instance, remote, path, content, message, branch, normalized);
                case "delete" -> deleteFile(instance, remote, path, message, branch, normalized);
                default -> ClientCommandResult.failure(
                        "GIT", normalized, "unsupported git action: " + action);
            };
        } catch (RuntimeException ex) {
            return ClientCommandResult.failure(
                    "GIT",
                    normalized,
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }

    private ClientCommandResult getFile(
            GitClientInstance instance,
            GiteaRemote remote,
            String path,
            String branch,
            String action) {
        requirePath(path);
        HttpResponse<String> response = send(
                instance,
                "GET",
                remote.apiContentsUrl(path) + "?ref=" + urlEncode(branch),
                null);
        if (response.statusCode() == 404) {
            return ClientCommandResult.failure(
                    "GIT", action, "file not found: " + path + " @ " + branch);
        }
        if (response.statusCode() >= 400) {
            return ClientCommandResult.failure(
                    "GIT",
                    action,
                    "Gitea GET HTTP " + response.statusCode() + ": " + response.body());
        }
        return ClientCommandResult.success(
                "GIT",
                action,
                "git file present: " + path,
                "{\"clientId\":\""
                        + escape(instance.id())
                        + "\",\"path\":\""
                        + escape(path)
                        + "\",\"branch\":\""
                        + escape(branch)
                        + "\",\"adapter\":\"gitea-http\",\"exists\":true}");
    }

    private ClientCommandResult upsertFile(
            GitClientInstance instance,
            GiteaRemote remote,
            String path,
            String content,
            String message,
            String branch,
            String action) {
        requirePath(path);
        if (content == null) {
            return ClientCommandResult.failure("GIT", action, "payload.content is required");
        }
        String sha = currentSha(instance, remote, path, branch);
        String body = "{"
                + "\"content\":\""
                + Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8))
                + "\","
                + "\"message\":\""
                + escape(message)
                + "\","
                + "\"branch\":\""
                + escape(branch)
                + "\""
                + (sha == null ? "" : ",\"sha\":\"" + escape(sha) + "\"")
                + "}";
        String method = sha == null ? "POST" : "PUT";
        HttpResponse<String> response =
                send(instance, method, remote.apiContentsUrl(path), body);
        if (response.statusCode() >= 400) {
            return ClientCommandResult.failure(
                    "GIT",
                    action,
                    "Gitea " + method + " HTTP " + response.statusCode() + ": " + response.body());
        }
        return ClientCommandResult.success(
                "GIT",
                action,
                "git file written: " + path,
                "{\"clientId\":\""
                        + escape(instance.id())
                        + "\",\"path\":\""
                        + escape(path)
                        + "\",\"branch\":\""
                        + escape(branch)
                        + "\",\"adapter\":\"gitea-http\"}");
    }

    private ClientCommandResult deleteFile(
            GitClientInstance instance,
            GiteaRemote remote,
            String path,
            String message,
            String branch,
            String action) {
        requirePath(path);
        String sha = currentSha(instance, remote, path, branch);
        if (sha == null) {
            return ClientCommandResult.success(
                    "GIT",
                    action,
                    "git file already absent: " + path,
                    "{\"clientId\":\""
                            + escape(instance.id())
                            + "\",\"path\":\""
                            + escape(path)
                            + "\",\"adapter\":\"gitea-http\",\"deleted\":false}");
        }
        String body = "{"
                + "\"message\":\""
                + escape(message)
                + "\","
                + "\"branch\":\""
                + escape(branch)
                + "\","
                + "\"sha\":\""
                + escape(sha)
                + "\"}";
        HttpResponse<String> response =
                send(instance, "DELETE", remote.apiContentsUrl(path), body);
        if (response.statusCode() >= 400) {
            return ClientCommandResult.failure(
                    "GIT",
                    action,
                    "Gitea DELETE HTTP " + response.statusCode() + ": " + response.body());
        }
        return ClientCommandResult.success(
                "GIT",
                action,
                "git file deleted: " + path,
                "{\"clientId\":\""
                        + escape(instance.id())
                        + "\",\"path\":\""
                        + escape(path)
                        + "\",\"adapter\":\"gitea-http\",\"deleted\":true}");
    }

    private String currentSha(
            GitClientInstance instance, GiteaRemote remote, String path, String branch) {
        HttpResponse<String> response = send(
                instance,
                "GET",
                remote.apiContentsUrl(path) + "?ref=" + urlEncode(branch),
                null);
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() >= 400) {
            throw new IllegalStateException(
                    "Gitea GET HTTP " + response.statusCode() + ": " + response.body());
        }
        return readStringField(response.body(), "sha");
    }

    private HttpResponse<String> send(
            GitClientInstance instance, String method, String url, String jsonBody) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .header(
                            "Authorization",
                            "Basic "
                                    + Base64.getEncoder()
                                            .encodeToString((instance.effectiveUsername()
                                                            + ":"
                                                            + instance.secret())
                                                    .getBytes(StandardCharsets.UTF_8)));
            if (jsonBody != null) {
                builder.header("Content-Type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                builder.GET();
            }
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            throw new IllegalStateException("Gitea HTTP " + method + " failed: " + ex.getMessage(), ex);
        }
    }

    private static void requirePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("payload.path is required");
        }
    }

    private static boolean payloadHasClient(String payloadJson) {
        String payload = payloadJson == null ? "{}" : payloadJson;
        String clientId = readStringField(payload, "clientId");
        return clientId != null && !clientId.isBlank();
    }

    private static GiteaRemote parseRemote(String remoteUrl) {
        Matcher matcher = REMOTE.matcher(remoteUrl == null ? "" : remoteUrl.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "unsupported git remote URL (expected http(s)://host/owner/repo): " + remoteUrl);
        }
        return new GiteaRemote(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    private static String normalize(String action) {
        return action == null || action.isBlank()
                ? "status"
                : action.trim().toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
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
        return value == null
                ? ""
                : value.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
    }

    private record GiteaRemote(String baseUrl, String owner, String repo) {
        String apiContentsUrl(String path) {
            String encodedPath = path.startsWith("/") ? path.substring(1) : path;
            return baseUrl + "/api/v1/repos/" + owner + "/" + repo + "/contents/" + encodedPath;
        }
    }
}
