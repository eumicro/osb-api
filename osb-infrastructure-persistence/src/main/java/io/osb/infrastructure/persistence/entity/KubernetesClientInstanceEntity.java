package io.osb.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "kubernetes_client_instances")
public class KubernetesClientInstanceEntity extends PanacheEntityBase {

    @Id
    @Column(length = 128)
    public String id;

    @Column(nullable = false, length = 255)
    public String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String description;

    @Column(name = "api_server_url", nullable = false, length = 1024)
    public String apiServerUrl;

    @Column(name = "default_namespace", nullable = false, length = 255)
    public String defaultNamespace;

    @Column(name = "auth_type", nullable = false, length = 64)
    public String authType;

    @Column(nullable = false, length = 255)
    public String username;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String token;

    @Column(name = "oauth_client_id", nullable = false, length = 255)
    public String oauthClientId;

    @Column(name = "oauth_client_secret", nullable = false, columnDefinition = "TEXT")
    public String oauthClientSecret;

    @Column(name = "well_known_url", nullable = false, length = 1024)
    public String wellKnownUrl;

    @Column(name = "insecure_skip_tls_verify", nullable = false)
    public boolean insecureSkipTlsVerify;

    @Column(name = "timeout_seconds", nullable = false)
    public int timeoutSeconds;

    @Column(nullable = false)
    public boolean enabled;
}
