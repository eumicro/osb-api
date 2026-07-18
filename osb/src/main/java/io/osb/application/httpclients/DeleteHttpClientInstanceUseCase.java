package io.osb.application.httpclients;

import io.osb.domain.httpclients.HttpClientInstanceRepository;
import java.util.Objects;

public final class DeleteHttpClientInstanceUseCase {

    private final HttpClientInstanceRepository repository;

    public DeleteHttpClientInstanceUseCase(HttpClientInstanceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void execute(String id) {
        if (!repository.delete(id)) {
            throw new IllegalArgumentException("http client not found: " + id);
        }
    }
}
