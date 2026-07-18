package io.osb.domain.templates;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository {

    List<Template> list();

    Optional<Template> findById(String id);

    void save(Template template);

    boolean delete(String id);
}
