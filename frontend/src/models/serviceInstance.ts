export type InstanceState =
  | "in progress"
  | "succeeded"
  | "failed"
  | "deleting"
  | "deprovisioned";

export type LastOperationState = "in progress" | "succeeded" | "failed" | string;

export interface ServiceInstance {
  id: string;
  serviceId: string;
  planId: string;
  state: InstanceState | string;
  platformClientId: string | null;
  dashboardUrl: string | null;
  parameters?: Record<string, unknown> | null;
  lastOperationId?: string | null;
  lastOperationState?: LastOperationState | null;
  lastOperationDescription?: string | null;
  lastOperationKind?: string | null;
}

export interface ProvisionInstanceRequest {
  serviceId: string;
  planId: string;
  platformClientId: string | null;
  parameters: Record<string, unknown>;
}

export interface UpdateInstanceRequest {
  platformClientId: string | null;
  dashboardUrl: string | null;
}

/** Instance is still running a lifecycle workflow (provision / deprovision). */
export function isInstanceBusy(instance: ServiceInstance): boolean {
  const state = (instance.state ?? "").toLowerCase();
  const op = (instance.lastOperationState ?? "").toLowerCase();
  return state === "in progress" || state === "deleting" || op === "in progress";
}

/** Deprovision finished successfully — safe to drop from the UI. */
export function isDeprovisionComplete(instance: ServiceInstance): boolean {
  if ((instance.state ?? "").toLowerCase() === "deprovisioned") {
    return true;
  }
  return (
    instance.lastOperationKind === "DEPROVISION"
    && (instance.lastOperationState ?? "").toLowerCase() === "succeeded"
  );
}
