package io.osb.workflow.n8n;

import io.osb.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class N8nOperationStore {

    record Entry(
            WorkflowStatus status,
            String dashboardUrl,
            String serviceId,
            String commandJson,
            boolean pollLastOperation) {}

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public void put(String operationId, WorkflowStatus status) {
        put(operationId, status, null);
    }

    public void put(String operationId, WorkflowStatus status, String dashboardUrl) {
        Entry previous = entries.get(operationId);
        String url = blankToNull(dashboardUrl);
        if (url == null && previous != null) {
            url = previous.dashboardUrl();
        }
        entries.put(operationId, new Entry(status, url, null, null, false));
    }

    /**
     * Provision apply finished; keep {@link WorkflowStatus#IN_PROGRESS} until
     * {@code INSTANCE_LAST_OPERATION} reports ready.
     */
    public void putPendingProvision(
            String operationId, String dashboardUrl, String serviceId, String commandJson) {
        entries.put(
                operationId,
                new Entry(
                        WorkflowStatus.IN_PROGRESS,
                        blankToNull(dashboardUrl),
                        serviceId,
                        commandJson,
                        true));
    }

    public Entry getEntry(String operationId) {
        return entries.get(operationId);
    }

    public WorkflowStatus get(String operationId) {
        Entry entry = entries.get(operationId);
        return entry == null ? WorkflowStatus.FAILED : entry.status();
    }

    public String dashboardUrl(String operationId) {
        Entry entry = entries.get(operationId);
        return entry == null ? null : entry.dashboardUrl();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
