package io.osb.application.templates;

import io.osb.domain.templates.TemplateRepository;
import java.util.Objects;

public final class DeleteTemplateUseCase {

    private final TemplateRepository repository;

    public DeleteTemplateUseCase(TemplateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void execute(String id) {
        if (!repository.delete(id)) {
            throw new IllegalArgumentException("template not found: " + id);
        }
    }
}
