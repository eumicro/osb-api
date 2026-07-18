package io.osb.domain.httpclients;

import java.util.Locale;
import java.util.Objects;

/**
 * Named, reusable HTTP client configuration for workflows (targets + auth).
 */
public final class HttpClientInstance {

    private final String id;
    private final String name;
    private final String description;
    private final String baseUrl;
    private final HttpClientAuthType authType;
    private final String username;
    private final String secret;
    private final String oauthClientId;
    private final String oauthClientSecret;
    private final String wellKnownUrl;
    private final int timeoutSeconds;
    private final boolean enabled;

    public HttpClientInstance(
            String id,
            String name,
            String description,
            String baseUrl,
            HttpClientAuthType authType,
            String username,
            String secret,
            String oauthClientId,
            String oauthClientSecret,
            String wellKnownUrl,
            int timeoutSeconds,
            boolean enabled) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = Objects.requireNonNullElse(description, "");
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.authType = Objects.requireNonNull(authType, "authType");
        this.username = Objects.requireNonNullElse(username, "").trim();
        this.secret = Objects.requireNonNullElse(secret, "");
        this.oauthClientId = Objects.requireNonNullElse(oauthClientId, "").trim();
        this.oauthClientSecret = Objects.requireNonNullElse(oauthClientSecret, "");
        this.wellKnownUrl = normalizeOptionalUrl(wellKnownUrl, "wellKnownUrl");
        if (timeoutSeconds < 1 || timeoutSeconds > 300) {
            throw new IllegalArgumentException("timeoutSeconds must be between 1 and 300");
        }
        this.timeoutSeconds = timeoutSeconds;
        this.enabled = enabled;
        validateAuth();
    }

    private void validateAuth() {
        if (authType == HttpClientAuthType.BASIC && username.isBlank()) {
            throw new IllegalArgumentException("username is required for BASIC auth");
        }
        if ((authType == HttpClientAuthType.BASIC || authType == HttpClientAuthType.BEARER)
                && secret.isBlank()) {
            throw new IllegalArgumentException("secret is required for " + authType);
        }
        if (authType == HttpClientAuthType.CLIENT_CREDENTIALS) {
            if (oauthClientId.isBlank()) {
                throw new IllegalArgumentException("oauthClientId is required for CLIENT_CREDENTIALS");
            }
            if (oauthClientSecret.isBlank()) {
                throw new IllegalArgumentException(
                        "oauthClientSecret is required for CLIENT_CREDENTIALS");
            }
            if (wellKnownUrl.isBlank()) {
                throw new IllegalArgumentException("wellKnownUrl is required for CLIENT_CREDENTIALS");
            }
        }
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public HttpClientAuthType authType() {
        return authType;
    }

    public String username() {
        return username;
    }

    public String secret() {
        return secret;
    }

    public String oauthClientId() {
        return oauthClientId;
    }

    public String oauthClientSecret() {
        return oauthClientSecret;
    }

    public String wellKnownUrl() {
        return wellKnownUrl;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean hasSecret() {
        return !secret.isBlank();
    }

    public boolean hasOauthClientSecret() {
        return !oauthClientSecret.isBlank();
    }

    public String resolveUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return baseUrl;
        }
        String path = pathOrUrl.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return baseUrl + path;
    }

    public HttpClientInstance withDetails(
            String name,
            String description,
            String baseUrl,
            HttpClientAuthType authType,
            String username,
            String secret,
            String oauthClientId,
            String oauthClientSecret,
            String wellKnownUrl,
            int timeoutSeconds,
            boolean enabled) {
        return new HttpClientInstance(
                id,
                name,
                description,
                baseUrl,
                authType,
                username,
                secret,
                oauthClientId,
                oauthClientSecret,
                wellKnownUrl,
                timeoutSeconds,
                enabled);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String value = requireText(baseUrl, "baseUrl").trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            throw new IllegalArgumentException("baseUrl must start with http:// or https://");
        }
        return value;
    }

    private static String normalizeOptionalUrl(String url, String field) {
        String value = Objects.requireNonNullElse(url, "").trim();
        if (value.isBlank()) {
            return "";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new IllegalArgumentException(field + " must start with http:// or https://");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
