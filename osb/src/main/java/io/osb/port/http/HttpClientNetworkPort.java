package io.osb.port.http;

import io.osb.port.client.ClientCommandResult;

/**
 * Infrastructure port for managed HTTP calls (n8n webhooks, external APIs).
 */
public interface HttpClientNetworkPort {

    String status();

    /**
     * POST JSON body to an absolute URL and return response body text.
     */
    String postJson(String url, String jsonBody);

    /**
     * Execute an HTTP action ({@code status}, {@code request}).
     */
    ClientCommandResult invoke(String action, String payloadJson);
}
