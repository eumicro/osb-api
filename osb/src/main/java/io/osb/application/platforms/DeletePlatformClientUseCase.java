package io.osb.application.platforms;

import io.osb.domain.platforms.PlatformClientRepository;
import java.util.Objects;

public final class DeletePlatformClientUseCase {

    private final PlatformClientRepository platformClientRepository;

    public DeletePlatformClientUseCase(PlatformClientRepository platformClientRepository) {
        this.platformClientRepository =
                Objects.requireNonNull(platformClientRepository, "platformClientRepository");
    }

    public void execute(String id) {
        if (!platformClientRepository.delete(id)) {
            throw new IllegalArgumentException("platform client not found: " + id);
        }
    }
}
