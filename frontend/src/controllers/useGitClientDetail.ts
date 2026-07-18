import { ref, watch, type Ref } from "vue";
import type { GitClientInstance, UpdateGitClientInstanceRequest } from "../models/gitClient";
import { gitClientService } from "../services/gitClientService";
import { notifyGitClientsChanged, selectGitClient, workspace } from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

export function useGitClientDetail(
  gitClientId: () => string | null,
  options: { syncWorkspace?: boolean } = {},
) {
  const syncWorkspace = options.syncWorkspace ?? true;
  const gitClient = ref<GitClientInstance | null>(null) as Ref<GitClientInstance | null>;
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const id = gitClientId();
    if (!id) {
      gitClient.value = null;
      if (syncWorkspace) workspace.selectedGitClient = null;
      return;
    }
    if (syncWorkspace && workspace.selectedGitClient?.id === id) {
      gitClient.value = workspace.selectedGitClient;
    }
    const result = await run(() => gitClientService.get(id));
    if (result) {
      gitClient.value = result;
      if (syncWorkspace) workspace.selectedGitClient = result;
    }
  }

  async function save(request: UpdateGitClientInstanceRequest): Promise<boolean> {
    const id = gitClientId();
    if (!id) return false;
    const saved = await run(() => gitClientService.update(id, request));
    if (saved) {
      gitClient.value = saved;
      if (syncWorkspace) selectGitClient(saved.id, saved);
      notifyGitClientsChanged();
      return true;
    }
    return false;
  }

  async function remove(): Promise<boolean> {
    const id = gitClientId();
    if (!id) return false;
    await run(() => gitClientService.delete(id));
    gitClient.value = null;
    if (syncWorkspace) selectGitClient(null);
    notifyGitClientsChanged();
    return error.value === "";
  }

  watch(
    () => [gitClientId(), workspace.gitClientRevision] as const,
    () => {
      void load();
    },
    { immediate: true },
  );

  return { gitClient, loading, error, load, save, remove };
}
