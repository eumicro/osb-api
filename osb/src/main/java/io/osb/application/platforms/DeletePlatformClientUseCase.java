package io.osb.application.platforms;

import io.osb.domain.platforms.PlatformClient;
import io.osb.domain.platforms.PlatformClientRepository;
import io.osb.domain.secrets.SecretRefs;
import io.osb.domain.secrets.SecretStore;
import java.util.Objects;

public final class DeletePlatformClientUseCase {

    private final PlatformClientRepository platformClientRepository;
    private final SecretStore secretStore;

    public DeletePlatformClientUseCase(
            PlatformClientRepository platformClientRepository, SecretStore secretStore) {
        this.platformClientRepository =
                Objects.requireNonNull(platformClientRepository, "platformClientRepository");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore");
    }

    public void execute(String id) {
        PlatformClient existing = platformClientRepository.findById(id).orElse(null);
        if (!platformClientRepository.delete(id)) {
            throw new IllegalArgumentException("platform client not found: " + id);
        }
        if (existing != null) {
            if (existing.passwordRef() != null && SecretRefs.isRef(existing.passwordRef())) {
                secretStore.delete(existing.passwordRef());
            }
            secretStore.delete(SecretRefs.platformPassword(id));
        }
    }
}
