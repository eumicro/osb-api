package io.osb.domain.httpclients;

import java.util.List;
import java.util.Optional;

public interface HttpClientInstanceRepository {

    List<HttpClientInstance> list();

    Optional<HttpClientInstance> findById(String id);

    void save(HttpClientInstance instance);

    boolean delete(String id);
}
