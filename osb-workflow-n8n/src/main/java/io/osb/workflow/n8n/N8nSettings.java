package io.osb.workflow.n8n;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class N8nSettings {

    private final String baseUrl;
    private final boolean invokeWebhooks;

    public N8nSettings(
            @ConfigProperty(name = "osb.n8n.base-url", defaultValue = "http://localhost:5678")
                    String baseUrl,
            @ConfigProperty(name = "osb.n8n.invoke-webhooks", defaultValue = "true")
                    boolean invokeWebhooks) {
        this.baseUrl = baseUrl;
        this.invokeWebhooks = invokeWebhooks;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public boolean invokeWebhooks() {
        return invokeWebhooks;
    }
}
