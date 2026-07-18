import { ref } from "vue";

/** Shared controller helper: loading/error around async actions. */
export function useAsyncAction() {
  const loading = ref(false);
  const error = ref("");

  async function run<T>(action: () => Promise<T>): Promise<T | undefined> {
    loading.value = true;
    error.value = "";
    try {
      return await action();
    } catch (e) {
      error.value = e instanceof Error ? e.message : String(e);
      return undefined;
    } finally {
      loading.value = false;
    }
  }

  return { loading, error, run };
}
