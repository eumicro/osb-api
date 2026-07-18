package io.osb.workflow.n8n;

import io.osb.workflow.WorkflowStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class N8nOperationStore {

    private record Entry(WorkflowStatus status, String dashboardUrl) {}

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public void put(String operationId, WorkflowStatus status) {
        put(operationId, status, null);
    }

    public void put(String operationId, WorkflowStatus status, String dashboardUrl) {
        entries.put(operationId, new Entry(status, blankToNull(dashboardUrl)));
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
