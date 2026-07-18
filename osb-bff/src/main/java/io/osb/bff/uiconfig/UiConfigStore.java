package io.osb.bff.uiconfig;

import java.util.Optional;

/** Port: per-user UI configuration storage (JSON documents). */
public interface UiConfigStore {

    Optional<String> find(String userName, String key);

    void save(String userName, String key, String json);

    boolean delete(String userName, String key);
}
