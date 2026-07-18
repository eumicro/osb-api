import { ref, watch, type Ref } from "vue";
import type { ServiceInstance, UpdateInstanceRequest } from "../models/serviceInstance";
import {
  isDeprovisionComplete,
  isInstanceBusy,
} from "../models/serviceInstance";
import { ApiError } from "../services/http";
import { instanceService } from "../services/instanceService";
import { applyInstanceSelection } from "../stores/selectionSync";
import {
  notifyInstancesChanged,
  selectInstance,
  workspace,
} from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

const DEPROVISION_POLL_MS = 800;
const DEPROVISION_POLL_MAX = 60;

/** Controller: Service-Instance-Detail. */
export function useInstanceDetail(
  instanceId: () => string | null,
  options: { syncWorkspace?: boolean } = {},
) {
  const syncWorkspace = options.syncWorkspace ?? true;
  const instance = ref<ServiceInstance | null>(null) as Ref<ServiceInstance | null>;
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const id = instanceId();
    if (!id) {
      instance.value = null;
      if (syncWorkspace) workspace.selectedInstance = null;
      return;
    }
    if (syncWorkspace && workspace.selectedInstance?.id === id) {
      instance.value = workspace.selectedInstance;
    }
    const result = await run(() => instanceService.get(id));
    if (result) {
      instance.value = result;
      if (syncWorkspace) workspace.selectedInstance = result;
    }
  }

  async function save(request: UpdateInstanceRequest): Promise<boolean> {
    const id = instanceId();
    if (!id) return false;
    const saved = await run(() => instanceService.update(id, request));
    if (saved) {
      instance.value = saved;
      if (syncWorkspace) {
        workspace.selectedInstanceId = saved.id;
        workspace.selectedInstance = saved;
      }
      notifyInstancesChanged();
      return true;
    }
    return false;
  }

  async function waitUntilDeprovisionSettled(id: string, initial: ServiceInstance) {
    let current = initial;
    instance.value = current;
    if (syncWorkspace) selectInstance(id, current);
    notifyInstancesChanged();

    if (isDeprovisionComplete(current) || current.state === "failed") {
      return current;
    }

    for (let i = 0; i < DEPROVISION_POLL_MAX; i++) {
      await new Promise((resolve) => setTimeout(resolve, DEPROVISION_POLL_MS));
      try {
        const next = await instanceService.get(id);
        current = next;
        instance.value = next;
        if (syncWorkspace) selectInstance(id, next);
        notifyInstancesChanged();
        if (isDeprovisionComplete(next) || next.state === "failed") {
          return next;
        }
        if (!isInstanceBusy(next) && next.lastOperationKind === "DEPROVISION") {
          return next;
        }
      } catch (err) {
        if (err instanceof ApiError && err.status === 404) {
          return { ...current, state: "deprovisioned" as const };
        }
        throw err;
      }
    }
    return current;
  }

  async function remove(): Promise<boolean> {
    const id = instanceId();
    if (!id) return false;
    const started = await run(() => instanceService.deprovision(id));
    if (!started) return false;

    const settled = await run(() => waitUntilDeprovisionSettled(id, started));
    if (!settled) return false;

    if (settled.state === "failed") {
      return false;
    }

    instance.value = null;
    if (syncWorkspace) applyInstanceSelection(null, [], []);
    notifyInstancesChanged();
    return error.value === "";
  }

  watch(
    () => [instanceId(), workspace.instanceRevision, workspace.platformRevision] as const,
    () => {
      void load();
    },
    { immediate: true },
  );

  return { instance, loading, error, load, save, remove };
}
