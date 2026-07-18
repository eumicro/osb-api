package io.osb.domain.gitclients;

import java.util.List;
import java.util.Optional;

public interface GitClientInstanceRepository {

    List<GitClientInstance> list();

    Optional<GitClientInstance> findById(String id);

    void save(GitClientInstance instance);

    boolean delete(String id);
}
