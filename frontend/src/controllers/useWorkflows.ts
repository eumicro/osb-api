import { computed, onMounted, ref, watch } from "vue";
import type { CreateWorkflowRequest, WorkflowDefinition } from "../models/workflow";
import { workflowService } from "../services/workflowService";
import {
  notifyWorkflowsChanged,
  selectWorkflow,
  workspace,
} from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

/** Controller: Workflow-Definitionen-Liste (serverseitig paginiert). */
export function useWorkflows() {
  const workflows = ref<WorkflowDefinition[]>([]);
  const page = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  const pageCount = ref(1);
  const { loading, error, run } = useAsyncAction();
  const selectedWorkflowId = computed(() => workspace.selectedWorkflowId);

  async function load() {
    const result = await run(() =>
      workflowService.list({ page: page.value, pageSize: pageSize.value }),
    );
    if (result) {
      workflows.value = result.items;
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

  function select(workflow: WorkflowDefinition) {
    selectWorkflow(workflow.id, workflow);
  }

  async function create(
    request: CreateWorkflowRequest,
  ): Promise<WorkflowDefinition | undefined> {
    const saved = await run(() => workflowService.create(request));
    if (saved) {
      notifyWorkflowsChanged();
      await load();
      selectWorkflow(saved.id, saved);
    }
    return saved;
  }

  async function remove(id: string) {
    await run(() => workflowService.remove(id));
    if (workspace.selectedWorkflowId === id) selectWorkflow(null);
    notifyWorkflowsChanged();
    await load();
  }

  onMounted(() => {
    void load();
  });

  watch(
    () => workspace.workflowRevision,
    () => {
      void load();
    },
  );

  return {
    workflows,
    page,
    pageSize,
    total,
    pageCount,
    selectedWorkflowId,
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
