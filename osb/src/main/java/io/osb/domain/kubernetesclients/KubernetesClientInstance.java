package io.osb.domain.kubernetesclients;

import java.util.Locale;
import java.util.Objects;

/**
 * Named Kubernetes API client (HTTP to apiServer) for workflows.
 */
public final class KubernetesClientInstance {

    private final String id;
    private final String name;
    private final String description;
    private final String apiServerUrl;
    private final String defaultNamespace;
    private final KubernetesClientAuthType authType;
    private final String username;
    private final String token;
    private final String oauthClientId;
    private final String oauthClientSecret;
    private final String wellKnownUrl;
    private final boolean insecureSkipTlsVerify;
    private final int timeoutSeconds;
    private final boolean enabled;

    public KubernetesClientInstance(
            String id,
            String name,
            String description,
            String apiServerUrl,
            String defaultNamespace,
            KubernetesClientAuthType authType,
            String username,
            String token,
            String oauthClientId,
            String oauthClientSecret,
            String wellKnownUrl,
            boolean insecureSkipTlsVerify,
            int timeoutSeconds,
            boolean enabled) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = Objects.requireNonNullElse(description, "");
        this.apiServerUrl = normalizeUrl(apiServerUrl);
        this.defaultNamespace = Objects.requireNonNullElse(defaultNamespace, "default").trim();
        if (this.defaultNamespace.isBlank()) {
            throw new IllegalArgumentException("defaultNamespace must not be blank");
        }
        this.authType = Objects.requireNonNull(authType, "authType");
        this.username = Objects.requireNonNullElse(username, "").trim();
        this.token = Objects.requireNonNullElse(token, "");
        this.oauthClientId = Objects.requireNonNullElse(oauthClientId, "").trim();
        this.oauthClientSecret = Objects.requireNonNullElse(oauthClientSecret, "");
        this.wellKnownUrl = normalizeOptionalUrl(wellKnownUrl, "wellKnownUrl");
        this.insecureSkipTlsVerify = insecureSkipTlsVerify;
        if (timeoutSeconds < 1 || timeoutSeconds > 300) {
            throw new IllegalArgumentException("timeoutSeconds must be between 1 and 300");
        }
        this.timeoutSeconds = timeoutSeconds;
        this.enabled = enabled;
        validateAuth();
    }

    private void validateAuth() {
        if (authType == KubernetesClientAuthType.BASIC && username.isBlank()) {
            throw new IllegalArgumentException("username is required for BASIC auth");
        }
        if ((authType == KubernetesClientAuthType.BASIC
                        || authType == KubernetesClientAuthType.BEARER)
                && token.isBlank()) {
            throw new IllegalArgumentException("token/password is required for " + authType);
        }
        if (authType == KubernetesClientAuthType.CLIENT_CREDENTIALS) {
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

    public String apiServerUrl() {
        return apiServerUrl;
    }

    public String defaultNamespace() {
        return defaultNamespace;
    }

    public KubernetesClientAuthType authType() {
        return authType;
    }

    public String username() {
        return username;
    }

    public String token() {
        return token;
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

    public boolean insecureSkipTlsVerify() {
        return insecureSkipTlsVerify;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean hasToken() {
        return !token.isBlank();
    }

    public boolean hasOauthClientSecret() {
        return !oauthClientSecret.isBlank();
    }

    public KubernetesClientInstance withDetails(
            String name,
            String description,
            String apiServerUrl,
            String defaultNamespace,
            KubernetesClientAuthType authType,
            String username,
            String token,
            String oauthClientId,
            String oauthClientSecret,
            String wellKnownUrl,
            boolean insecureSkipTlsVerify,
            int timeoutSeconds,
            boolean enabled) {
        return new KubernetesClientInstance(
                id,
                name,
                description,
                apiServerUrl,
                defaultNamespace,
                authType,
                username,
                token,
                oauthClientId,
                oauthClientSecret,
                wellKnownUrl,
                insecureSkipTlsVerify,
                timeoutSeconds,
                enabled);
    }

    private static String normalizeUrl(String url) {
        String value = requireText(url, "apiServerUrl").trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            throw new IllegalArgumentException("apiServerUrl must start with http:// or https://");
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
