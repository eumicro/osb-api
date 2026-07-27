package io.osb.domain.secrets;

import java.util.Objects;

/**
 * Stable secret-reference convention: {@code osb/{type}/{clientId}/{field}}.
 */
public final class SecretRefs {

    public static final String PREFIX = "osb/";

    public static final String TYPE_HTTP = "http";
    public static final String TYPE_GIT = "git";
    public static final String TYPE_K8S = "k8s";
    public static final String TYPE_PLATFORM = "platform";

    public static final String FIELD_SECRET = "secret";
    public static final String FIELD_OAUTH_CLIENT_SECRET = "oauthClientSecret";
    public static final String FIELD_PASSPHRASE = "passphrase";
    public static final String FIELD_TOKEN = "token";
    public static final String FIELD_PASSWORD = "password";

    private SecretRefs() {}

    public static boolean isRef(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public static String httpSecret(String clientId) {
        return ref(TYPE_HTTP, clientId, FIELD_SECRET);
    }

    public static String httpOauthClientSecret(String clientId) {
        return ref(TYPE_HTTP, clientId, FIELD_OAUTH_CLIENT_SECRET);
    }

    public static String gitSecret(String clientId) {
        return ref(TYPE_GIT, clientId, FIELD_SECRET);
    }

    public static String gitPassphrase(String clientId) {
        return ref(TYPE_GIT, clientId, FIELD_PASSPHRASE);
    }

    public static String k8sToken(String clientId) {
        return ref(TYPE_K8S, clientId, FIELD_TOKEN);
    }

    public static String k8sOauthClientSecret(String clientId) {
        return ref(TYPE_K8S, clientId, FIELD_OAUTH_CLIENT_SECRET);
    }

    public static String platformPassword(String clientId) {
        return ref(TYPE_PLATFORM, clientId, FIELD_PASSWORD);
    }

    public static String ref(String type, String clientId, String field) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(field, "field");
        if (type.isBlank() || clientId.isBlank() || field.isBlank()) {
            throw new IllegalArgumentException("type, clientId and field must not be blank");
        }
        return PREFIX + type + "/" + clientId.trim() + "/" + field.trim();
    }
}
