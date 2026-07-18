package io.osb.port.client;

/**
 * Result of invoking an infrastructure client from a workflow engine (n8n).
 */
public record ClientCommandResult(
        boolean ok, String client, String action, String message, String detailsJson) {

    public static ClientCommandResult success(
            String client, String action, String message, String detailsJson) {
        return new ClientCommandResult(
                true, client, action, message, detailsJson == null ? "{}" : detailsJson);
    }

    public static ClientCommandResult failure(String client, String action, String message) {
        return new ClientCommandResult(false, client, action, message, "{}");
    }
}
