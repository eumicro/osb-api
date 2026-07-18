package io.osb.domain.platforms;

import java.util.List;
import java.util.Optional;

public interface PlatformClientRepository {

    List<PlatformClient> list();

    Optional<PlatformClient> findById(String id);

    Optional<PlatformClient> findByUsername(String username);

    void save(PlatformClient platformClient);

    boolean delete(String id);
}
