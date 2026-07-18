import type { DockviewApi } from "dockview-vue";
import { i18n } from "../i18n";

/**
 * Registry aller Workspace-Panels (domain-ui Muster).
 * `list` / `dashboard` erscheinen in der Navigation; `detail` nur per Selektion.
 */
export type PanelKind = "shell" | "dashboard" | "list" | "detail";

export interface PanelDefinition {
  id: string;
  component: string;
  titleKey: string;
  kind: PanelKind;
}

export const PANELS: readonly PanelDefinition[] = [
  { id: "navigation", component: "NavigationPanel", titleKey: "panels.navigation", kind: "shell" },
  { id: "dashboard", component: "DashboardPanel", titleKey: "panels.dashboard", kind: "dashboard" },
  { id: "catalogs", component: "CatalogsPanel", titleKey: "panels.catalogs", kind: "list" },
  { id: "catalog", component: "CatalogPanel", titleKey: "panels.catalog", kind: "detail" },
  { id: "offerings", component: "OfferingsPanel", titleKey: "panels.offerings", kind: "list" },
  { id: "offering", component: "OfferingPanel", titleKey: "panels.offering", kind: "detail" },
  { id: "plans", component: "PlansPanel", titleKey: "panels.plans", kind: "list" },
  { id: "plan", component: "PlanPanel", titleKey: "panels.plan", kind: "detail" },
  { id: "instances", component: "InstancesPanel", titleKey: "panels.instances", kind: "list" },
  { id: "instance", component: "InstancePanel", titleKey: "panels.instance", kind: "detail" },
  { id: "platforms", component: "PlatformsPanel", titleKey: "panels.platforms", kind: "list" },
  { id: "platform", component: "PlatformPanel", titleKey: "panels.platform", kind: "detail" },
  { id: "workflows", component: "WorkflowsPanel", titleKey: "panels.workflows", kind: "list" },
  { id: "workflow", component: "WorkflowPanel", titleKey: "panels.workflow", kind: "detail" },
  { id: "httpClients", component: "HttpClientsPanel", titleKey: "panels.httpClients", kind: "list" },
  { id: "httpClient", component: "HttpClientPanel", titleKey: "panels.httpClient", kind: "detail" },
  {
    id: "kubernetesClients",
    component: "KubernetesClientsPanel",
    titleKey: "panels.kubernetesClients",
    kind: "list",
  },
  {
    id: "kubernetesClient",
    component: "KubernetesClientPanel",
    titleKey: "panels.kubernetesClient",
    kind: "detail",
  },
  { id: "gitClients", component: "GitClientsPanel", titleKey: "panels.gitClients", kind: "list" },
  { id: "gitClient", component: "GitClientPanel", titleKey: "panels.gitClient", kind: "detail" },
  { id: "templates", component: "TemplatesPanel", titleKey: "panels.templates", kind: "list" },
  { id: "template", component: "TemplatePanel", titleKey: "panels.template", kind: "detail" },
];

/** Navigation: nur Dashboard und Listenfenster. */
export const NAVIGATION_PANELS: readonly PanelDefinition[] = PANELS.filter(
  (panel) => panel.kind === "dashboard" || panel.kind === "list",
);

export function panelTitle(titleKey: string): string {
  return String(i18n.global.t(titleKey));
}

export function refreshPanelTitles(api: DockviewApi) {
  for (const panel of api.panels) {
    if (
      panel.id.startsWith("catalog:")
      || panel.id.startsWith("offering:")
      || panel.id.startsWith("plan:")
      || panel.id.startsWith("instance:")
      || panel.id.startsWith("platform:")
      || panel.id.startsWith("workflow:")
      || panel.id.startsWith("httpClient:")
      || panel.id.startsWith("kubernetesClient:")
      || panel.id.startsWith("gitClient:")
      || panel.id.startsWith("template:")
    ) {
      continue;
    }
    const definition = PANELS.find((entry) => entry.id === panel.id);
    if (definition) {
      panel.api.setTitle(panelTitle(definition.titleKey));
    }
  }
}

export function buildSinglePanelLayout(api: DockviewApi, panelId: string) {
  api.clear();
  const definition = PANELS.find((panel) => panel.id === panelId);
  if (definition) {
    api.addPanel({
      id: definition.id,
      component: definition.component,
      title: panelTitle(definition.titleKey),
    });
  }
}

export interface ViewPreset {
  id: string;
  build: (api: DockviewApi) => void;
}

/** Vordefinierte Layout-Vorlagen beim Anlegen einer neuen Ansicht. */
export const VIEW_PRESETS: readonly ViewPreset[] = [
  { id: "current", build: () => {} },
  { id: "default", build: buildDefaultLayout },
  ...PANELS.map((panel) => ({
    id: panel.id,
    build: (api: DockviewApi) => buildSinglePanelLayout(api, panel.id),
  })),
];

/**
 * Standard-Layout (Seed):
 * Kataloge | Katalog · Pläne · Offerings · Offering · Workflows · Templates · Template
 */
export function buildDefaultLayout(api: DockviewApi) {
  api.addPanel({
    id: "catalogs",
    component: "CatalogsPanel",
    title: panelTitle("panels.catalogs"),
  });
  api.addPanel({
    id: "catalog",
    component: "CatalogPanel",
    title: panelTitle("panels.catalog"),
    position: { referencePanel: "catalogs", direction: "right" },
    inactive: true,
  });
  api.addPanel({
    id: "plans",
    component: "PlansPanel",
    title: panelTitle("panels.plans"),
    position: { referencePanel: "catalog", direction: "within" },
    inactive: true,
  });
  api.addPanel({
    id: "offerings",
    component: "OfferingsPanel",
    title: panelTitle("panels.offerings"),
    position: { referencePanel: "catalog", direction: "within" },
    inactive: true,
  });
  api.addPanel({
    id: "offering",
    component: "OfferingPanel",
    title: panelTitle("panels.offering"),
    position: { referencePanel: "catalog", direction: "within" },
    inactive: true,
  });
  api.addPanel({
    id: "workflows",
    component: "WorkflowsPanel",
    title: panelTitle("panels.workflows"),
    position: { referencePanel: "catalog", direction: "within" },
    inactive: true,
  });
  api.addPanel({
    id: "templates",
    component: "TemplatesPanel",
    title: panelTitle("panels.templates"),
    position: { referencePanel: "catalog", direction: "within" },
    inactive: true,
  });
  api.addPanel({
    id: "template",
    component: "TemplatePanel",
    title: panelTitle("panels.template"),
    position: { referencePanel: "catalog", direction: "within" },
  });
}
