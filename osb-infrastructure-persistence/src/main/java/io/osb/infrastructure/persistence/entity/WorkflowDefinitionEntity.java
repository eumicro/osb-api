package io.osb.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "workflow_definitions")
public class WorkflowDefinitionEntity extends PanacheEntityBase {

    @Id
    @Column(length = 128)
    public String id;

    @Column(nullable = false, length = 255)
    public String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String description;

    @Column(nullable = false, length = 32)
    public String kind;

    @Column(name = "n8n_webhook_path", nullable = false, length = 512)
    public String n8nWebhookPath;

    @Column(name = "n8n_workflow_id", nullable = false, length = 255)
    public String n8nWorkflowId;

    @Column(nullable = false)
    public boolean enabled;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "workflow_client_types",
            joinColumns = @JoinColumn(name = "workflow_id"))
    @Column(name = "client_type", nullable = false, length = 32)
    public Set<String> clientTypes = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "workflow_http_clients",
            joinColumns = @JoinColumn(name = "workflow_id"))
    @Column(name = "http_client_id", nullable = false, length = 128)
    public Set<String> httpClientIds = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "workflow_kubernetes_clients",
            joinColumns = @JoinColumn(name = "workflow_id"))
    @Column(name = "kubernetes_client_id", nullable = false, length = 128)
    public Set<String> kubernetesClientIds = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "workflow_git_clients",
            joinColumns = @JoinColumn(name = "workflow_id"))
    @Column(name = "git_client_id", nullable = false, length = 128)
    public Set<String> gitClientIds = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "workflow_templates",
            joinColumns = @JoinColumn(name = "workflow_id"))
    @Column(name = "template_id", nullable = false, length = 128)
    public Set<String> templateIds = new LinkedHashSet<>();
}
