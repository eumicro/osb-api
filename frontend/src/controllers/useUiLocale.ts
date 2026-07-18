import { ref } from "vue";
import { useI18n } from "vue-i18n";
import {
  applyDocumentLocale,
  LOCALE_STORAGE_KEY,
  SUPPORTED_LOCALES,
  type AppLocale,
} from "../i18n";
import { uiConfigService } from "../services/uiConfigService";

const LOCALE_CONFIG_KEY = "ui-locale";

/**
 * Controller: UI language. Prefer BFF ui-config; fall back to localStorage.
 */
export function useUiLocale() {
  const { locale } = useI18n({ useScope: "global" });
  const ready = ref(false);

  async function init() {
    const saved = await uiConfigService.load<AppLocale>(LOCALE_CONFIG_KEY).catch(() => null);
    if (saved && SUPPORTED_LOCALES.includes(saved)) {
      locale.value = saved;
    } else {
      const local = localStorage.getItem(LOCALE_STORAGE_KEY) as AppLocale | null;
      if (local && SUPPORTED_LOCALES.includes(local)) {
        locale.value = local;
      }
    }
    applyDocumentLocale(locale.value as AppLocale);
    ready.value = true;
  }

  async function setLocale(next: AppLocale) {
    locale.value = next;
    applyDocumentLocale(next);
    localStorage.setItem(LOCALE_STORAGE_KEY, next);
    await uiConfigService.save(LOCALE_CONFIG_KEY, next).catch(() => undefined);
  }

  void init();

  return { locale, ready, setLocale };
}
