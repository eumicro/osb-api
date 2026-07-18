package io.osb.domain.gitclients;

import java.util.Locale;
import java.util.Objects;

/**
 * Named Git remote configuration for workflows (HTTPS or SSH).
 */
public final class GitClientInstance {

    private final String id;
    private final String name;
    private final String description;
    private final String remoteUrl;
    private final String defaultBranch;
    private final GitClientAuthMethod authMethod;
    private final String username;
    private final String secret;
    private final String passphrase;
    private final boolean enabled;

    public GitClientInstance(
            String id,
            String name,
            String description,
            String remoteUrl,
            String defaultBranch,
            GitClientAuthMethod authMethod,
            String username,
            String secret,
            String passphrase,
            boolean enabled) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.description = Objects.requireNonNullElse(description, "");
        this.authMethod = Objects.requireNonNull(authMethod, "authMethod");
        this.remoteUrl = normalizeRemoteUrl(remoteUrl, authMethod);
        this.defaultBranch = Objects.requireNonNullElse(defaultBranch, "main").trim();
        if (this.defaultBranch.isBlank()) {
            throw new IllegalArgumentException("defaultBranch must not be blank");
        }
        this.username = Objects.requireNonNullElse(username, "").trim();
        this.secret = Objects.requireNonNullElse(secret, "");
        this.passphrase = Objects.requireNonNullElse(passphrase, "");
        this.enabled = enabled;
        if (this.secret.isBlank()) {
            throw new IllegalArgumentException(
                    authMethod == GitClientAuthMethod.HTTPS
                            ? "password or token is required for HTTPS"
                            : "private key is required for SSH");
        }
        if (authMethod == GitClientAuthMethod.SSH && this.username.isBlank()) {
            // git@host uses user "git"; allow blank and treat as "git" at use time
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

    public String remoteUrl() {
        return remoteUrl;
    }

    public String defaultBranch() {
        return defaultBranch;
    }

    public GitClientAuthMethod authMethod() {
        return authMethod;
    }

    public String username() {
        return username;
    }

    /** Effective SSH user (defaults to {@code git} when blank). */
    public String effectiveUsername() {
        if (!username.isBlank()) {
            return username;
        }
        return authMethod == GitClientAuthMethod.SSH ? "git" : username;
    }

    /**
     * HTTPS: password or personal access token. SSH: private key (PEM).
     */
    public String secret() {
        return secret;
    }

    /** Optional passphrase for an encrypted SSH private key. */
    public String passphrase() {
        return passphrase;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean hasSecret() {
        return !secret.isBlank();
    }

    public boolean hasPassphrase() {
        return !passphrase.isBlank();
    }

    /**
     * Resolve a repository path or absolute URL against this client's remote base.
     * Absolute {@code http(s)://} / {@code git@} / {@code ssh://} values are returned unchanged.
     */
    public String resolveRemote(String repositoryOrUrl) {
        if (repositoryOrUrl == null || repositoryOrUrl.isBlank()) {
            return remoteUrl;
        }
        String value = repositoryOrUrl.trim();
        if (value.startsWith("http://")
                || value.startsWith("https://")
                || value.startsWith("git@")
                || value.startsWith("ssh://")) {
            return value;
        }
        String base = remoteUrl;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (authMethod == GitClientAuthMethod.SSH && base.startsWith("git@") && !base.contains(":")) {
            throw new IllegalArgumentException("invalid SSH remoteUrl: " + base);
        }
        // SSH scp-style: git@host:org/repo.git — append path after host:
        if (authMethod == GitClientAuthMethod.SSH && base.startsWith("git@")) {
            String path = value.startsWith("/") ? value.substring(1) : value;
            if (base.endsWith(".git") || base.contains("/")) {
                int colon = base.indexOf(':');
                if (colon > 0) {
                    String hostPart = base.substring(0, colon + 1);
                    return hostPart + path;
                }
            }
            return base.endsWith(":") ? base + path : base + ":" + path;
        }
        String path = value.startsWith("/") ? value.substring(1) : value;
        return base + "/" + path;
    }

    public GitClientInstance withDetails(
            String name,
            String description,
            String remoteUrl,
            String defaultBranch,
            GitClientAuthMethod authMethod,
            String username,
            String secret,
            String passphrase,
            boolean enabled) {
        return new GitClientInstance(
                id,
                name,
                description,
                remoteUrl,
                defaultBranch,
                authMethod,
                username,
                secret,
                passphrase,
                enabled);
    }

    private static String normalizeRemoteUrl(String url, GitClientAuthMethod authMethod) {
        String value = requireText(url, "remoteUrl").trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (authMethod == GitClientAuthMethod.HTTPS) {
            if (!(lower.startsWith("http://") || lower.startsWith("https://"))) {
                throw new IllegalArgumentException(
                        "HTTPS remoteUrl must start with http:// or https://");
            }
        } else {
            if (!(lower.startsWith("git@") || lower.startsWith("ssh://"))) {
                throw new IllegalArgumentException(
                        "SSH remoteUrl must start with git@ or ssh://");
            }
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
