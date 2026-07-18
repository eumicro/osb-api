package io.osb.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "platform_clients")
public class PlatformClientEntity extends PanacheEntityBase {

    @Id
    @Column(length = 128)
    public String id;

    @Column(name = "display_name", nullable = false, length = 255)
    public String displayName;

    @Column(nullable = false, unique = true, length = 255)
    public String username;

    @Column(name = "catalog_id", nullable = false, length = 128)
    public String catalogId;

    @Column(nullable = false)
    public boolean enabled;
}
