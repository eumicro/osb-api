import { ref, watch, type Ref } from "vue";
import type { Template, UpdateTemplateRequest } from "../models/template";
import { templateService } from "../services/templateService";
import { notifyTemplatesChanged, selectTemplate, workspace } from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

export function useTemplateDetail(
  templateId: () => string | null,
  options: { syncWorkspace?: boolean } = {},
) {
  const syncWorkspace = options.syncWorkspace ?? true;
  const template = ref<Template | null>(null) as Ref<Template | null>;
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const id = templateId();
    if (!id) {
      template.value = null;
      if (syncWorkspace) workspace.selectedTemplate = null;
      return;
    }
    if (syncWorkspace && workspace.selectedTemplate?.id === id) {
      template.value = workspace.selectedTemplate;
    }
    const result = await run(() => templateService.get(id));
    if (result) {
      template.value = result;
      if (syncWorkspace) workspace.selectedTemplate = result;
    }
  }

  async function save(request: UpdateTemplateRequest): Promise<boolean> {
    const id = templateId();
    if (!id) return false;
    const saved = await run(() => templateService.update(id, request));
    if (saved) {
      template.value = saved;
      if (syncWorkspace) selectTemplate(saved.id, saved);
      notifyTemplatesChanged();
      return true;
    }
    return false;
  }

  async function remove(): Promise<boolean> {
    const id = templateId();
    if (!id) return false;
    await run(() => templateService.remove(id));
    template.value = null;
    if (syncWorkspace) selectTemplate(null);
    notifyTemplatesChanged();
    return true;
  }

  watch(
    () => [templateId(), workspace.templateRevision] as const,
    () => {
      void load();
    },
    { immediate: true },
  );

  return { template, loading, error, save, remove, load };
}
