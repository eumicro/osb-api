package io.osb.application.templates;

import io.osb.domain.templates.Template;
import io.osb.domain.templates.TemplateRepository;
import java.util.Objects;

public final class GetTemplateUseCase {

    private final TemplateRepository repository;

    public GetTemplateUseCase(TemplateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Template execute(String id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("template not found: " + id));
    }
}
