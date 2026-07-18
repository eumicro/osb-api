import { ref } from "vue";
import { uiConfigService } from "../services/uiConfigService";
import {
  applyTheme,
  detectSystemTheme,
  THEME_OPTIONS,
  THEME_STORAGE_KEY,
  type UiTheme,
} from "../theme";

const THEME_CONFIG_KEY = "ui-theme";

/** Controller: light/dark theme via BFF ui-config with localStorage fallback. */
export function useUiTheme() {
  const theme = ref<UiTheme>(detectSystemTheme());

  async function init() {
    const saved = await uiConfigService.load<UiTheme>(THEME_CONFIG_KEY).catch(() => null);
    if (saved && THEME_OPTIONS.includes(saved)) {
      theme.value = saved;
    } else {
      const local = localStorage.getItem(THEME_STORAGE_KEY) as UiTheme | null;
      if (local && THEME_OPTIONS.includes(local)) {
        theme.value = local;
      }
    }
    applyTheme(theme.value);
  }

  async function setTheme(next: UiTheme) {
    theme.value = next;
    applyTheme(next);
    localStorage.setItem(THEME_STORAGE_KEY, next);
    await uiConfigService.save(THEME_CONFIG_KEY, next).catch(() => undefined);
  }

  void init();

  return { theme, setTheme };
}
