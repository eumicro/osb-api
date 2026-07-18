import type { Component } from "vue";

/**
 * Plugin contract for extending the OSB UI (nav, pages, dashboard widgets).
 * First cut: registration + dashboard widgets only.
 */
export interface OsbPlugin {
  id: string;
  navLabelKey?: string;
  dashboardWidget?: Component;
  setup?: () => void | Promise<void>;
}
