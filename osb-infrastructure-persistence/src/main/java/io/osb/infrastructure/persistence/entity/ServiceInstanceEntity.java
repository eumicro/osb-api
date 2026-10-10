package io.osb.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "service_instances")
public class ServiceInstanceEntity extends PanacheEntityBase {

    @Id
    @Column(length = 128)
    public String id;

    @Column(name = "service_id", nullable = false, length = 128)
    public String serviceId;

    @Column(name = "plan_id", nullable = false, length = 128)
    public String planId;

    @Column(nullable = false, length = 64)
    public String state;

    @Column(name = "platform_client_id", length = 128)
    public String platformClientId;

    @Column(name = "dashboard_url", length = 2048)
    public String dashboardUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    public Map<String, Object> parameters = new LinkedHashMap<>();

    @Column(name = "last_operation_id", length = 128)
    public String lastOperationId;

    @Column(name = "last_operation_state", length = 64)
    public String lastOperationState;

    @Column(name = "last_operation_description", columnDefinition = "TEXT")
    public String lastOperationDescription;

    @Column(name = "last_operation_kind", length = 32)
    public String lastOperationKind;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (parameters == null) {
            parameters = new LinkedHashMap<>();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
