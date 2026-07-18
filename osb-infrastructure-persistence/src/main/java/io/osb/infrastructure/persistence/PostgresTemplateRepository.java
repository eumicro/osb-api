package io.osb.infrastructure.persistence;

import io.osb.domain.templates.Template;
import io.osb.domain.templates.TemplateRepository;
import io.osb.infrastructure.persistence.entity.TemplateEntity;
import io.osb.infrastructure.persistence.mapper.TemplateMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostgresTemplateRepository implements TemplateRepository {

    @Override
    public List<Template> list() {
        return TemplateEntity.<TemplateEntity>listAll().stream()
                .map(TemplateMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Template> findById(String id) {
        return Optional.ofNullable(TemplateEntity.<TemplateEntity>findById(id))
                .map(TemplateMapper::toDomain);
    }

    @Override
    @Transactional
    public void save(Template template) {
        TemplateEntity entity = TemplateEntity.findById(template.id());
        if (entity == null) {
            TemplateMapper.toNewEntity(template).persist();
            return;
        }
        TemplateMapper.apply(template, entity);
    }

    @Override
    @Transactional
    public boolean delete(String id) {
        return TemplateEntity.deleteById(id);
    }
}
