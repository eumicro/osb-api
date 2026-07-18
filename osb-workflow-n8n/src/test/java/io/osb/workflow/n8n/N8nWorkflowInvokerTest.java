package io.osb.workflow.n8n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.osb.workflow.WorkflowStatus;
import org.junit.jupiter.api.Test;

class N8nWorkflowInvokerTest {

    @Test
    void readsDashboardUrlFromWorkflowResponse() {
        assertEquals(
                "http://redis-ui-inst-1.localhost:8088/",
                N8nWorkflowInvoker.readDashboardUrl(
                        "{\"ok\":true,\"dashboardUrl\":\"http://redis-ui-inst-1.localhost:8088/\"}"));
        assertEquals(
                "http://example/dash",
                N8nWorkflowInvoker.readDashboardUrl(
                        "{\"ok\":true,\"dashboard_url\":\"http://example/dash\"}"));
        assertNull(N8nWorkflowInvoker.readDashboardUrl("{\"ok\":true}"));
    }

    @Test
    void detectsExplicitOkFalse() {
        assertTrue(N8nWorkflowInvoker.isExplicitFailure("{\"ok\":false,\"message\":\"x\"}"));
        assertFalse(N8nWorkflowInvoker.isExplicitFailure("{\"ok\":true}"));
        assertFalse(N8nWorkflowInvoker.isExplicitFailure("{}"));
    }

    @Test
    void readsExplicitWorkflowState() {
        assertEquals(
                WorkflowStatus.IN_PROGRESS,
                N8nWorkflowInvoker.readOperationState(
                        "{\"ok\":true,\"state\":\"in progress\"}"));
        assertEquals(
                WorkflowStatus.SUCCEEDED,
                N8nWorkflowInvoker.readOperationState("{\"ok\":true,\"state\":\"succeeded\"}"));
        assertEquals(
                WorkflowStatus.FAILED,
                N8nWorkflowInvoker.readOperationState("{\"ok\":true,\"state\":\"failed\"}"));
    }

    @Test
    void missingStateMeansSynchronousSuccess() {
        assertEquals(
                WorkflowStatus.SUCCEEDED,
                N8nWorkflowInvoker.readOperationState("{\"ok\":true,\"scenario\":\"git\"}"));
    }
}
