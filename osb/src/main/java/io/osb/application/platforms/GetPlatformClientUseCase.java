package io.osb.application.platforms;

import io.osb.domain.platforms.PlatformClient;
import io.osb.domain.platforms.PlatformClientRepository;
import java.util.Objects;

public final class GetPlatformClientUseCase {

    private final PlatformClientRepository platformClientRepository;

    public GetPlatformClientUseCase(PlatformClientRepository platformClientRepository) {
        this.platformClientRepository =
                Objects.requireNonNull(platformClientRepository, "platformClientRepository");
    }

    public PlatformClient execute(String id) {
        return platformClientRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("platform client not found: " + id));
    }
}
