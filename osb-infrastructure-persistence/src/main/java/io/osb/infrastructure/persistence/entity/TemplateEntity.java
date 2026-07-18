package io.osb.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "templates")
public class TemplateEntity extends PanacheEntityBase {

    @Id
    @Column(length = 128)
    public String id;

    @Column(nullable = false, length = 255)
    public String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String description;

    @Column(nullable = false, length = 64)
    public String kind;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String content;

    @Column(nullable = false)
    public boolean enabled;
}
