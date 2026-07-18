import { onMounted, ref, watch } from "vue";
import type { CreateTemplateRequest, Template } from "../models/template";
import { templateService } from "../services/templateService";
import { notifyTemplatesChanged, selectTemplate, workspace } from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

export function useTemplates() {
  const templates = ref<Template[]>([]);
  const page = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  const pageCount = ref(1);
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const result = await run(() =>
      templateService.list({ page: page.value, pageSize: pageSize.value }),
    );
    if (result) {
      templates.value = result.items;
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

  async function create(request: CreateTemplateRequest): Promise<Template | undefined> {
    const saved = await run(() => templateService.create(request));
    if (saved) {
      notifyTemplatesChanged();
      await load();
      selectTemplate(saved.id, saved);
    }
    return saved;
  }

  async function remove(id: string) {
    await run(() => templateService.remove(id));
    if (workspace.selectedTemplateId === id) selectTemplate(null);
    notifyTemplatesChanged();
    await load();
  }

  onMounted(() => {
    void load();
  });

  watch(
    () => workspace.templateRevision,
    () => {
      void load();
    },
  );

  return {
    templates,
    page,
    pageSize,
    total,
    pageCount,
    loading,
    error,
    load,
    setPage,
    setPageSize,
    create,
    remove,
  };
}
