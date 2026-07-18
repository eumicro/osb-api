import { computed, onMounted, ref, watch } from "vue";
import type { ProvisionInstanceRequest, ServiceInstance } from "../models/serviceInstance";
import {
  isDeprovisionComplete,
  isInstanceBusy,
} from "../models/serviceInstance";
import { ADMIN_LOOKUP_PAGE_SIZE } from "../models/page";
import { ApiError } from "../services/http";
import { instanceService } from "../services/instanceService";
import {
  notifyInstancesChanged,
  selectInstance,
  workspace,
} from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

const DEPROVISION_POLL_MS = 800;
const DEPROVISION_POLL_MAX = 60;

/** Controller: Service-Instance-Liste (optional serverseitig paginiert). */
export function useInstances(options: { paged?: boolean } = {}) {
  const paged = options.paged ?? false;
  const instances = ref<ServiceInstance[]>([]);
  const page = ref(1);
  const pageSize = ref(paged ? 10 : ADMIN_LOOKUP_PAGE_SIZE);
  const total = ref(0);
  const pageCount = ref(1);
  const { loading, error, run } = useAsyncAction();
  const selectedInstanceId = computed(() => workspace.selectedInstanceId);

  async function load() {
    const result = await run(() =>
      instanceService.list({ page: page.value, pageSize: pageSize.value }),
    );
    if (result) {
      instances.value = result.items;
      page.value = result.page;
      pageSize.value = result.pageSize;
      total.value = result.total;
      pageCount.value = result.pageCount;
    }
  }

  function setPage(next: number) {
    page.value = next;
    void load();
  }

  function setPageSize(size: number) {
    pageSize.value = size;
    page.value = 1;
    void load();
  }

  function select(instance: ServiceInstance) {
    selectInstance(instance.id, instance);
  }

  async function create(
    request: ProvisionInstanceRequest,
  ): Promise<ServiceInstance | undefined> {
    const saved = await run(() => instanceService.provision(request));
    if (saved) {
      notifyInstancesChanged();
      await load();
      selectInstance(saved.id, saved);
    }
    return saved;
  }

  function patchLocal(instance: ServiceInstance) {
    const idx = instances.value.findIndex((item) => item.id === instance.id);
    if (idx >= 0) {
      const next = instances.value.slice();
      next[idx] = instance;
      instances.value = next;
    }
    if (workspace.selectedInstanceId === instance.id) {
      selectInstance(instance.id, instance);
    }
  }

  async function waitUntilDeprovisionSettled(id: string, initial: ServiceInstance) {
    let current = initial;
    patchLocal(current);
    notifyInstancesChanged();

    if (isDeprovisionComplete(current) || current.state === "failed") {
      return current;
    }

    for (let i = 0; i < DEPROVISION_POLL_MAX; i++) {
      await new Promise((resolve) => setTimeout(resolve, DEPROVISION_POLL_MS));
      try {
        const next = await instanceService.get(id);
        current = next;
        patchLocal(next);
        notifyInstancesChanged();
        if (isDeprovisionComplete(next) || next.state === "failed") {
          return next;
        }
        if (!isInstanceBusy(next) && next.lastOperationKind === "DEPROVISION") {
          return next;
        }
      } catch (err) {
        if (err instanceof ApiError && err.status === 404) {
          // Purged after successful deprovision.
          return { ...current, state: "deprovisioned" };
        }
        throw err;
      }
    }
    return current;
  }

  /**
   * Starts deprovision and keeps the instance in the UI until last-operation
   * succeeds (or the instance is gone / failed).
   */
  async function remove(id: string) {
    const started = await run(() => instanceService.deprovision(id));
    if (!started) {
      return;
    }

    const settled = await run(() => waitUntilDeprovisionSettled(id, started));
    if (!settled) {
      return;
    }

    if (settled.state === "failed") {
      await load();
      return;
    }

    // Only now drop from the UI.
    if (workspace.selectedInstanceId === id) {
      selectInstance(null);
    }
    notifyInstancesChanged();
    await load();
  }

  onMounted(() => {
    void load();
  });

  watch(
    () => [workspace.instanceRevision, workspace.platformRevision] as const,
    () => {
      void load();
    },
  );

  return {
    instances,
    page,
    pageSize,
    total,
    pageCount,
    selectedInstanceId,
    loading,
    error,
    load,
    setPage,
    setPageSize,
    select,
    create,
    remove,
  };
}
