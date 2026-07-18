/**
 * OSB lifecycle operations that can be orchestrated via n8n.
 * Catalog (GET /v2/catalog) is not a workflow kind.
 */
export type WorkflowKind =
  | "PROVISION"
  | "DEPROVISION"
  | "UPDATE"
  | "BIND"
  | "UNBIND"
  | "INSTANCE_LAST_OPERATION"
  | "BINDING_LAST_OPERATION"
  | "GET_INSTANCE"
  | "GET_BINDING";

export type WorkflowClientType = "GIT" | "KUBERNETES" | "HTTP";

export interface WorkflowDefinition {
  id: string;
  name: string;
  description: string;
  kind: WorkflowKind;
  n8nWebhookPath: string;
  n8nWorkflowId: string;
  enabled: boolean;
  clients: WorkflowClientType[];
  httpClientIds: string[];
  kubernetesClientIds: string[];
  gitClientIds: string[];
}

export interface CreateWorkflowRequest {
  name: string;
  description: string;
  kind: WorkflowKind;
  n8nWebhookPath: string;
  n8nWorkflowId: string;
  enabled: boolean;
  clients: WorkflowClientType[];
  httpClientIds: string[];
  kubernetesClientIds: string[];
  gitClientIds: string[];
}

export interface UpdateWorkflowRequest {
  name: string;
  description: string;
  kind: WorkflowKind;
  n8nWebhookPath: string;
  n8nWorkflowId: string;
  enabled: boolean;
  clients: WorkflowClientType[];
  httpClientIds: string[];
  kubernetesClientIds: string[];
  gitClientIds: string[];
}

export interface WorkflowClientStatus {
  type: WorkflowClientType;
  status: string;
}

export const WORKFLOW_CLIENT_TYPES: readonly WorkflowClientType[] = [
  "GIT",
  "KUBERNETES",
  "HTTP",
];

export const WORKFLOW_KINDS: readonly WorkflowKind[] = [
  "PROVISION",
  "DEPROVISION",
  "UPDATE",
  "BIND",
  "UNBIND",
  "INSTANCE_LAST_OPERATION",
  "BINDING_LAST_OPERATION",
  "GET_INSTANCE",
  "GET_BINDING",
];

/** Suggested n8n webhook path / workflow id when creating a workflow of a given kind. */
export const WORKFLOW_KIND_DEFAULTS: Record<
  WorkflowKind,
  { webhookPath: string; n8nWorkflowId: string }
> = {
  PROVISION: {
    webhookPath: "/webhook/osb-provision",
    n8nWorkflowId: "osbWfProvision",
  },
  DEPROVISION: {
    webhookPath: "/webhook/osb-deprovision",
    n8nWorkflowId: "osbWfDeprovision",
  },
  UPDATE: {
    webhookPath: "/webhook/osb-update",
    n8nWorkflowId: "osbWfUpdate",
  },
  BIND: {
    webhookPath: "/webhook/osb-bind",
    n8nWorkflowId: "osbWfBind",
  },
  UNBIND: {
    webhookPath: "/webhook/osb-unbind",
    n8nWorkflowId: "osbWfUnbind",
  },
  INSTANCE_LAST_OPERATION: {
    webhookPath: "/webhook/osb-instance-last-operation",
    n8nWorkflowId: "osbWfInstanceLastOp",
  },
  BINDING_LAST_OPERATION: {
    webhookPath: "/webhook/osb-binding-last-operation",
    n8nWorkflowId: "osbWfBindingLastOp",
  },
  GET_INSTANCE: {
    webhookPath: "/webhook/osb-get-instance",
    n8nWorkflowId: "osbWfGetInstance",
  },
  GET_BINDING: {
    webhookPath: "/webhook/osb-get-binding",
    n8nWorkflowId: "osbWfGetBinding",
  },
};
