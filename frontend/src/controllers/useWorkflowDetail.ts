import { ref, watch, type Ref } from "vue";
import type {
  UpdateWorkflowRequest,
  WorkflowClientStatus,
  WorkflowDefinition,
} from "../models/workflow";
import { workflowService } from "../services/workflowService";
import {
  notifyWorkflowsChanged,
  selectWorkflow,
  workspace,
} from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

/** Controller: Workflow-Detail inkl. Client-Status. */
export function useWorkflowDetail(
  workflowId: () => string | null,
  options: { syncWorkspace?: boolean } = {},
) {
  const syncWorkspace = options.syncWorkspace ?? true;
  const workflow = ref<WorkflowDefinition | null>(null) as Ref<WorkflowDefinition | null>;
  const clients = ref<WorkflowClientStatus[]>([]);
  const { loading, error, run } = useAsyncAction();

  async function loadClients() {
    const result = await run(() => workflowService.listClients());
    if (result) clients.value = result;
  }

  async function load() {
    const id = workflowId();
    if (!id) {
      workflow.value = null;
      if (syncWorkspace) workspace.selectedWorkflow = null;
      return;
    }
    if (syncWorkspace && workspace.selectedWorkflow?.id === id) {
      workflow.value = workspace.selectedWorkflow;
    }
    const result = await run(() => workflowService.get(id));
    if (result) {
      workflow.value = result;
      if (syncWorkspace) workspace.selectedWorkflow = result;
    }
  }

  async function save(request: UpdateWorkflowRequest): Promise<boolean> {
    const id = workflowId();
    if (!id) return false;
    const saved = await run(() => workflowService.update(id, request));
    if (saved) {
      workflow.value = saved;
      if (syncWorkspace) selectWorkflow(saved.id, saved);
      notifyWorkflowsChanged();
      return true;
    }
    return false;
  }

  async function remove(): Promise<boolean> {
    const id = workflowId();
    if (!id) return false;
    await run(() => workflowService.remove(id));
    workflow.value = null;
    if (syncWorkspace) selectWorkflow(null);
    notifyWorkflowsChanged();
    return error.value === "";
  }

  watch(
    () => [workflowId(), workspace.workflowRevision] as const,
    () => {
      void load();
    },
    { immediate: true },
  );

  return { workflow, clients, loading, error, load, loadClients, save, remove };
}
