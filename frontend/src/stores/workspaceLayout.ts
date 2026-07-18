import type { DockviewApi } from "dockview-vue";
import type { AdminCatalog, ServiceOffering } from "../models/catalog";
import type { GitClientInstance } from "../models/gitClient";
import type { HttpClientInstance } from "../models/httpClient";
import type { KubernetesClientInstance } from "../models/kubernetesClient";
import type { PlanDetail } from "../models/plan";
import type { PlatformClient } from "../models/platformClient";
import type { ServiceInstance } from "../models/serviceInstance";
import type { Template } from "../models/template";
import type { WorkflowDefinition } from "../models/workflow";
import { i18n } from "../i18n";
import { PANELS, panelTitle } from "../workspace/panels";

let dockviewApi: DockviewApi | null = null;

export function registerDockviewApi(api: DockviewApi | null) {
  dockviewApi = api;
}

export function focusPanel(panelId: string) {
  if (!dockviewApi) return;
  const existing = dockviewApi.getPanel(panelId);
  if (existing) {
    existing.api.setActive();
    return;
  }
  const definition = PANELS.find((panel) => panel.id === panelId);
  if (!definition) return;
  dockviewApi.addPanel({
    id: definition.id,
    component: definition.component,
    title: panelTitle(definition.titleKey),
  });
}

export interface CatalogPanelParams {
  catalogId: string;
}

export interface OfferingPanelParams {
  catalogId: string;
  offeringId: string;
}

export interface PlatformPanelParams {
  platformId: string;
}

export interface PlanPanelParams {
  catalogId: string;
  offeringId: string;
  planId: string;
}

export interface InstancePanelParams {
  instanceId: string;
}

export interface WorkflowPanelParams {
  workflowId: string;
}

export interface HttpClientPanelParams {
  httpClientId: string;
}

export interface KubernetesClientPanelParams {
  kubernetesClientId: string;
}

export interface GitClientPanelParams {
  gitClientId: string;
}

export interface TemplatePanelParams {
  templateId: string;
}

export function catalogPanelId(catalogId: string): string {
  return `catalog:${catalogId}`;
}

export function offeringPanelId(offeringId: string): string {
  return `offering:${offeringId}`;
}

export function openCatalogInNewTab(catalog: AdminCatalog) {
  if (!dockviewApi) return;
  const panelId = catalogPanelId(catalog.id);
  const existing = dockviewApi.getPanel(panelId);
  if (existing) {
    existing.api.setActive();
    return;
  }
  const params: CatalogPanelParams = { catalogId: catalog.id };
  dockviewApi.addPanel({
    id: panelId,
    component: "CatalogPanel",
    title: catalog.name,
    params,
    position: dockviewApi.getPanel("catalog")
      ? { referencePanel: "catalog", direction: "within" }
      : undefined,
  });
}

export function openOfferingInNewTab(catalogId: string, offering: ServiceOffering) {
  if (!dockviewApi) return;
  const panelId = offeringPanelId(offering.id);
  const existing = dockviewApi.getPanel(panelId);
  if (existing) {
    existing.api.setActive();
    return;
  }
  const params: OfferingPanelParams = { catalogId, offeringId: offering.id };
  dockviewApi.addPanel({
    id: panelId,
    component: "OfferingPanel",
    title: offering.name,
    params,
    position: dockviewApi.getPanel("offering")
      ? { referencePanel: "offering", direction: "within" }
      : undefined,
  });
}

export function planPanelId(offeringId: string, planId: string): string {
  return `plan:${offeringId}:${planId}`;
}

export function openPlanInNewTab(catalogId: string, plan: PlanDetail) {
  if (!dockviewApi) return;
  const panelId = planPanelId(plan.serviceId, plan.id);
  const existing = dockviewApi.getPanel(panelId);
  if (existing) {
    existing.api.setActive();
    return;
  }
  const params: PlanPanelParams = {
    catalogId,
    offeringId: plan.serviceId,
    planId: plan.id,
  };
  dockviewApi.addPanel({
    id: panelId,
    component: "PlanPanel",
    title: plan.name,
    params,
    position: dockviewApi.getPanel("plan")
      ? { referencePanel: "plan", direction: "within" }
      : undefined,
  });
}

export function instancePanelId(instanceId: string): string {
  return `instance:${instanceId}`;
}

export function openInstanceInNewTab(instance: ServiceInstance) {
  if (!dockviewApi) return;
  const panelId = instancePanelId(instance.id);
  const existing = dockviewApi.getPanel(panelId);
  if (existing) {
    existing.api.setActive();
    return;
  }
  const params: InstancePanelParams = { instanceId: instance.id };
  dockviewApi.addPanel({
    id: panelId,
    component: "InstancePanel",
    title: instance.id,
    params,
    position: dockviewApi.getPanel("instance")
      ? { referencePanel: "instance", direction: "within" }
      : undefined,
  });
}

export function platformPanelId(platformId: string): string {
  return `platform:${platformId}`;
}

export function openPlatformInNewTab(platform: PlatformClient) {
  if (!dockviewApi) return;
  const panelId = platformPanelId(platform.id);
  const existing = dockviewApi.getPanel(panelId);
  if (existing) {
    existing.api.setActive();
    return;
  }
  const params: PlatformPanelParams = { platformId: platform.id };
  dockviewApi.addPanel({
    id: panelId,
    component: "PlatformPanel",
    title: platform.displayName,
    params,
    position: dockviewApi.getPanel("platform")
      ? { referencePanel: "platform", direction: "within" }
      : undefined,
  });
}

export function workflowPanelId(workflowId: string): string {
  return `workflow:${workflowId}`;
}

export function openWorkflowInNewTab(workflow: WorkflowDefinition) {
  if (!dockviewApi) return;
  const panelId = workflowPanelId(workflow.id);
  const existing = dockviewApi.getPanel(panelId);
  if (existing) {
    existing.api.setActive();
    return;
  }
  const params: WorkflowPanelParams = { workflowId: workflow.id };
  dockviewApi.addPanel({
    id: panelId,
    component: "WorkflowPanel",
    title: workflow.name,
    params,
    position: dockviewApi.getPanel("workflow")
      ? { referencePanel: "workflow", direction: "within" }
      : undefined,
  });
}

export function httpClientPanelId(httpClientId: string): string {
  return `httpClient:${httpClientId}`;
}

export function openHttpClientInNewTab(httpClient: HttpClientInstance) {
  if (!dockviewApi) return;
  const panelId = httpClientPanelId(httpClient.id);
  const existing = dockviewApi.getPanel(panelId);
  if (existing) {
    existing.api.setActive();
    return;
  }
  const params: HttpClientPanelParams = { httpClientId: httpClient.id };
  dockviewApi.addPanel({
    id: panelId,
    component: "HttpClientPanel",
    title: httpClient.name,
    params,
    position: dockviewApi.getPanel("httpClient")
      ? { referencePanel: "httpClient", direction: "within" }
      : undefined,
  });
}

export function kubernetesClientPanelId(kubernetesClientId: string): string {
  return `kubernetesClient:${kubernetesClientId}`;
}

export function openKubernetesClientInNewTab(kubernetesClient: KubernetesClientInstance) {
  if (!dockviewApi) return;
  const panelId = kubernetesClientPanelId(kubernetesClient.id);
  const existing = dockviewApi.getPanel(panelId);
  if (existing) {
    existing.api.setActive();
    return;
  }
  const params: KubernetesClientPanelParams = {
    kubernetesClientId: kubernetesClient.id,
  };
  dockviewApi.addPanel({
    id: panelId,
    component: "KubernetesClientPanel",
    title: kubernetesClient.name,
    params,
    position: dockviewApi.getPanel("kubernetesClient")
      ? { referencePanel: "kubernetesClient", direction: "within" }
      : undefined,
  });
}

export function gitClientPanelId(gitClientId: string): string {
  return `gitClient:${gitClientId}`;
}

export function openGitClientInNewTab(gitClient: GitClientInstance) {
  if (!dockviewApi) return;
  const panelId = gitClientPanelId(gitClient.id);
  const existing = dockviewApi.getPanel(panelId);
  if (existing) {
    existing.api.setActive();
    return;
  }
  const params: GitClientPanelParams = { gitClientId: gitClient.id };
  dockviewApi.addPanel({
    id: panelId,
    component: "GitClientPanel",
    title: gitClient.name,
    params,
    position: dockviewApi.getPanel("gitClient")
      ? { referencePanel: "gitClient", direction: "within" }
      : undefined,
  });
}

export function templatePanelId(templateId: string): string {
  return `template:${templateId}`;
}

export function openTemplateInNewTab(template: Template) {
  if (!dockviewApi) return;
  const panelId = templatePanelId(template.id);
  const existing = dockviewApi.getPanel(panelId);
  if (existing) {
    existing.api.setActive();
    return;
  }
  const params: TemplatePanelParams = { templateId: template.id };
  dockviewApi.addPanel({
    id: panelId,
    component: "TemplatePanel",
    title: template.name,
    params,
    position: dockviewApi.getPanel("template")
      ? { referencePanel: "template", direction: "within" }
      : undefined,
  });
}

export function refreshDynamicPanelTitles(api: DockviewApi) {
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
      // Bound tabs keep their item name; locale changes leave them as-is.
      continue;
    }
    const definition = PANELS.find((entry) => entry.id === panel.id);
    if (definition) {
      panel.api.setTitle(String(i18n.global.t(definition.titleKey)));
    }
  }
}
