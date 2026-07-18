package io.osb.application.templates;

import io.osb.domain.templates.Template;
import io.osb.domain.templates.TemplateKind;
import io.osb.domain.templates.TemplateRepository;
import java.util.Objects;
import java.util.UUID;

public final class SaveTemplateUseCase {

    private final TemplateRepository repository;

    public SaveTemplateUseCase(TemplateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Template create(
            String name, String description, TemplateKind kind, String content, boolean enabled) {
        Template created = new Template(
                "tpl-" + UUID.randomUUID().toString().substring(0, 8),
                name,
                description,
                kind,
                content,
                enabled);
        repository.save(created);
        return created;
    }

    public Template update(
            String id,
            String name,
            String description,
            TemplateKind kind,
            String content,
            boolean enabled) {
        Template existing = repository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("template not found: " + id));
        Template updated = existing.withDetails(name, description, kind, content, enabled);
        repository.save(updated);
        return updated;
    }
}
