import { themeDark, themeLight, type DockviewTheme } from "dockview-vue";

export type UiTheme = "light" | "dark";

export const THEME_STORAGE_KEY = "osb.ui-theme";

export const THEME_OPTIONS: readonly UiTheme[] = ["light", "dark"];

export function detectSystemTheme(): UiTheme {
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

/** Applies color scheme to the document (CSS variables + color-scheme). */
export function applyTheme(theme: UiTheme) {
  document.documentElement.dataset.theme = theme;
  document.documentElement.style.colorScheme = theme;
}

/** Dockview-Theme passend zur UI-Darstellung (ohne :theme-Prop bleibt Dockview dunkel). */
export function dockviewThemeFor(theme: UiTheme): DockviewTheme {
  return theme === "dark" ? themeDark : themeLight;
}
