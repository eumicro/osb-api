package io.osb.domain.workflows;

/**
 * Maps to Open Service Broker API operations that can be orchestrated via n8n.
 *
 * <p>Catalog ({@code GET /v2/catalog}) is not a workflow kind — it is served directly by the API.
 */
public enum WorkflowKind {
    /** {@code PUT /v2/service_instances/{instance_id}} */
    PROVISION,
    /** {@code DELETE /v2/service_instances/{instance_id}} */
    DEPROVISION,
    /** {@code PATCH /v2/service_instances/{instance_id}} */
    UPDATE,
    /** {@code PUT /v2/service_instances/{instance_id}/service_bindings/{binding_id}} */
    BIND,
    /** {@code DELETE /v2/service_instances/{instance_id}/service_bindings/{binding_id}} */
    UNBIND,
    /** {@code GET /v2/service_instances/{instance_id}/last_operation} */
    INSTANCE_LAST_OPERATION,
    /** {@code GET /v2/service_instances/{instance_id}/service_bindings/{binding_id}/last_operation} */
    BINDING_LAST_OPERATION,
    /** {@code GET /v2/service_instances/{instance_id}} */
    GET_INSTANCE,
    /** {@code GET /v2/service_instances/{instance_id}/service_bindings/{binding_id}} */
    GET_BINDING
}
