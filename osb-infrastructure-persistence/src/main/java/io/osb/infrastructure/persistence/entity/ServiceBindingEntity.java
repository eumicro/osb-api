package io.osb.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "service_bindings")
@IdClass(ServiceBindingEntity.Pk.class)
public class ServiceBindingEntity extends PanacheEntityBase {

    @Id
    @Column(length = 128)
    public String id;

    @Id
    @Column(name = "instance_id", length = 128)
    public String instanceId;

    @Column(name = "service_id", nullable = false, length = 128)
    public String serviceId;

    @Column(name = "plan_id", nullable = false, length = 128)
    public String planId;

    @Column(name = "platform_client_id", length = 128)
    public String platformClientId;

    @Column(nullable = false, length = 64)
    public String state;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    public Map<String, Object> credentials = new LinkedHashMap<>();

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
        if (credentials == null) {
            credentials = new LinkedHashMap<>();
        }
        if (parameters == null) {
            parameters = new LinkedHashMap<>();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public static class Pk implements Serializable {
        public String id;
        public String instanceId;

        public Pk() {}

        public Pk(String id, String instanceId) {
            this.id = id;
            this.instanceId = instanceId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pk pk)) {
                return false;
            }
            return Objects.equals(id, pk.id) && Objects.equals(instanceId, pk.instanceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, instanceId);
        }
    }
}
