import { ApiError, http } from "./http";

/** Per-user UI config via BFF (`/bff/ui-config/{key}`). */
export const uiConfigService = {
  async load<T>(key: string): Promise<T | null> {
    try {
      return await http.get<T>(`/bff/ui-config/${encodeURIComponent(key)}`);
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) return null;
      throw e;
    }
  },

  save: (key: string, value: unknown) =>
    http.put<void>(`/bff/ui-config/${encodeURIComponent(key)}`, value),

  async remove(key: string): Promise<void> {
    try {
      await http.delete<void>(`/bff/ui-config/${encodeURIComponent(key)}`);
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) return;
      throw e;
    }
  },
};
