package io.osb.infrastructure.persistence.mapper;

import io.osb.domain.templates.Template;
import io.osb.domain.templates.TemplateKind;
import io.osb.infrastructure.persistence.entity.TemplateEntity;

public final class TemplateMapper {

    private TemplateMapper() {}

    public static Template toDomain(TemplateEntity entity) {
        return new Template(
                entity.id,
                entity.name,
                entity.description,
                TemplateKind.valueOf(entity.kind),
                entity.content,
                entity.enabled);
    }

    public static TemplateEntity toNewEntity(Template template) {
        TemplateEntity entity = new TemplateEntity();
        entity.id = template.id();
        apply(template, entity);
        return entity;
    }

    public static void apply(Template template, TemplateEntity entity) {
        entity.name = template.name();
        entity.description = template.description();
        entity.kind = template.kind().name();
        entity.content = template.content();
        entity.enabled = template.enabled();
    }
}
