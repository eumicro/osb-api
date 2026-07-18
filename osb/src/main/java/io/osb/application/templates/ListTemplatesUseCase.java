package io.osb.application.templates;

import io.osb.domain.templates.Template;
import io.osb.domain.templates.TemplateRepository;
import java.util.List;
import java.util.Objects;

public final class ListTemplatesUseCase {

    private final TemplateRepository repository;

    public ListTemplatesUseCase(TemplateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<Template> execute() {
        return repository.list();
    }
}
