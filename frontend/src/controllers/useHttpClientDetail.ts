import { ref, watch, type Ref } from "vue";
import type {
  HttpClientInstance,
  UpdateHttpClientInstanceRequest,
} from "../models/httpClient";
import { httpClientService } from "../services/httpClientService";
import {
  notifyHttpClientsChanged,
  selectHttpClient,
  workspace,
} from "../stores/workspace";
import { useAsyncAction } from "./useAsyncAction";

export function useHttpClientDetail(
  httpClientId: () => string | null,
  options: { syncWorkspace?: boolean } = {},
) {
  const syncWorkspace = options.syncWorkspace ?? true;
  const httpClient = ref<HttpClientInstance | null>(null) as Ref<HttpClientInstance | null>;
  const { loading, error, run } = useAsyncAction();

  async function load() {
    const id = httpClientId();
    if (!id) {
      httpClient.value = null;
      if (syncWorkspace) workspace.selectedHttpClient = null;
      return;
    }
    if (syncWorkspace && workspace.selectedHttpClient?.id === id) {
      httpClient.value = workspace.selectedHttpClient;
    }
    const result = await run(() => httpClientService.get(id));
    if (result) {
      httpClient.value = result;
      if (syncWorkspace) workspace.selectedHttpClient = result;
    }
  }

  async function save(request: UpdateHttpClientInstanceRequest): Promise<boolean> {
    const id = httpClientId();
    if (!id) return false;
    const saved = await run(() => httpClientService.update(id, request));
    if (saved) {
      httpClient.value = saved;
      if (syncWorkspace) selectHttpClient(saved.id, saved);
      notifyHttpClientsChanged();
      return true;
    }
    return false;
  }

  async function remove(): Promise<boolean> {
    const id = httpClientId();
    if (!id) return false;
    await run(() => httpClientService.delete(id));
    httpClient.value = null;
    if (syncWorkspace) selectHttpClient(null);
    notifyHttpClientsChanged();
    return error.value === "";
  }

  watch(
    () => [httpClientId(), workspace.httpClientRevision] as const,
    () => {
      void load();
    },
    { immediate: true },
  );

  return { httpClient, loading, error, load, save, remove };
}
