package io.osb.port.git;

import io.osb.port.client.ClientCommandResult;

/**
 * Infrastructure port for Git operations (workflow + later UI commands).
 */
public interface GitClientPort {

    String status();

    /**
     * Execute a Git action ({@code status}, {@code clone}, {@code commit}, {@code push},
     * {@code checkout}).
     */
    ClientCommandResult invoke(String action, String payloadJson);
}
