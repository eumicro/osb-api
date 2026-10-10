package io.osb.workflow;

import java.util.Map;

public interface BindingWorkflow {

    String start(BindingCommand command);

    WorkflowStatus status(String operationId);

    /** Optional credentials from a completed bind operation. */
    default Map<String, Object> credentials(String operationId) {
        return Map.of();
    }
}
