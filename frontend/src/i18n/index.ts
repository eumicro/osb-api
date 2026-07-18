import { createI18n } from "vue-i18n";
import de from "./locales/de";
import en from "./locales/en";
import es from "./locales/es";
import fr from "./locales/fr";
import pl from "./locales/pl";
import ru from "./locales/ru";

export type AppLocale = "de" | "en" | "fr" | "pl" | "es" | "ru";

export const SUPPORTED_LOCALES: readonly AppLocale[] = ["de", "en", "fr", "pl", "es", "ru"];

export const LOCALE_LABELS: Record<AppLocale, string> = {
  de: "Deutsch",
  en: "English",
  fr: "Français",
  pl: "Polski",
  es: "Español",
  ru: "Русский",
};

/** Country flags used in the locale switcher (emoji). */
export const LOCALE_FLAGS: Record<AppLocale, string> = {
  de: "🇩🇪",
  en: "🇬🇧",
  fr: "🇫🇷",
  pl: "🇵🇱",
  es: "🇪🇸",
  ru: "🇷🇺",
};

export const LOCALE_STORAGE_KEY = "osb.ui-locale";

const BROWSER_LOCALE_MAP: Record<string, AppLocale> = {
  de: "de",
  en: "en",
  fr: "fr",
  pl: "pl",
  es: "es",
  ru: "ru",
};

function detectBrowserLocale(): AppLocale {
  const tag = navigator.language?.toLowerCase() ?? "de";
  const primary = tag.split("-")[0] ?? "de";
  return BROWSER_LOCALE_MAP[primary] ?? "en";
}

export const i18n = createI18n({
  legacy: false,
  locale: detectBrowserLocale(),
  fallbackLocale: "de",
  messages: { de, en, fr, pl, es, ru },
});

export function applyDocumentLocale(locale: AppLocale) {
  document.documentElement.lang = locale;
  document.title = String(i18n.global.t("app.pageTitle"));
}
