package io.osb.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "git_client_instances")
public class GitClientInstanceEntity extends PanacheEntityBase {

    @Id
    @Column(length = 128)
    public String id;

    @Column(nullable = false, length = 255)
    public String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String description;

    @Column(name = "remote_url", nullable = false, length = 1024)
    public String remoteUrl;

    @Column(name = "default_branch", nullable = false, length = 255)
    public String defaultBranch;

    @Column(name = "auth_method", nullable = false, length = 32)
    public String authMethod;

    @Column(nullable = false, length = 255)
    public String username;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String secret;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String passphrase;

    @Column(nullable = false)
    public boolean enabled;
}
