package io.osb.port.kubernetes;

import io.osb.port.client.ClientCommandResult;

/**
 * Infrastructure port for Kubernetes resource operations.
 */
public interface KubernetesClientPort {

    String status();

    /**
     * Execute a Kubernetes action ({@code status}, {@code apply}, {@code delete}, {@code get}).
     */
    ClientCommandResult invoke(String action, String payloadJson);
}
