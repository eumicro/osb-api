package io.osb.application.httpclients;

import io.osb.domain.httpclients.HttpClientInstance;
import io.osb.domain.httpclients.HttpClientInstanceRepository;
import java.util.List;
import java.util.Objects;

public final class ListHttpClientInstancesUseCase {

    private final HttpClientInstanceRepository repository;

    public ListHttpClientInstancesUseCase(HttpClientInstanceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<HttpClientInstance> execute() {
        return repository.list();
    }
}
