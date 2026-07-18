import { ref } from "vue";
import type { Me } from "../models/me";
import { meService } from "../services/meService";

export function useMe() {
  const me = ref<Me | null>(null);

  async function refresh() {
    try {
      me.value = await meService.get();
    } catch {
      me.value = null;
    }
  }

  return { me, refresh };
}
