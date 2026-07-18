package io.osb.application.platforms;

import io.osb.domain.platforms.PlatformClient;
import io.osb.domain.platforms.PlatformClientRepository;
import java.util.List;
import java.util.Objects;

public final class ListPlatformClientsUseCase {

    private final PlatformClientRepository platformClientRepository;

    public ListPlatformClientsUseCase(PlatformClientRepository platformClientRepository) {
        this.platformClientRepository =
                Objects.requireNonNull(platformClientRepository, "platformClientRepository");
    }

    public List<PlatformClient> execute() {
        return platformClientRepository.list();
    }
}
