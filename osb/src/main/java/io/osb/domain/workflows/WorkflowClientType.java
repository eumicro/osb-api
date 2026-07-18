package io.osb.domain.workflows;

/** Infrastructure clients a workflow may use during orchestration. */
public enum WorkflowClientType {
    GIT,
    KUBERNETES,
    HTTP
}
