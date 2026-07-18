package io.osb.application.httpclients;

import io.osb.domain.httpclients.HttpClientInstance;
import io.osb.domain.httpclients.HttpClientInstanceRepository;
import java.util.Objects;

public final class GetHttpClientInstanceUseCase {

    private final HttpClientInstanceRepository repository;

    public GetHttpClientInstanceUseCase(HttpClientInstanceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public HttpClientInstance execute(String id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("http client not found: " + id));
    }
}
