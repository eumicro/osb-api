import { reactive } from "vue";
import type { AdminCatalog, ServiceOffering } from "../models/catalog";
import type { GitClientInstance } from "../models/gitClient";
import type { HttpClientInstance } from "../models/httpClient";
import type { KubernetesClientInstance } from "../models/kubernetesClient";
import type { PlanDetail } from "../models/plan";
import type { PlatformClient } from "../models/platformClient";
import type { ServiceInstance } from "../models/serviceInstance";
import type { Template } from "../models/template";
import type { WorkflowDefinition } from "../models/workflow";

/**
 * Shared workspace selection (domain-ui Muster).
 * Panels communicate via this selection, not direct references.
 */
export const workspace = reactive({
  selectedCatalogId: null as string | null,
  selectedCatalog: null as AdminCatalog | null,
  catalogRevision: 0,
  selectedOfferingId: null as string | null,
  selectedOffering: null as ServiceOffering | null,
  offeringRevision: 0,
  selectedPlanId: null as string | null,
  selectedPlanOfferingId: null as string | null,
  selectedPlan: null as PlanDetail | null,
  selectedInstanceId: null as string | null,
  selectedInstance: null as ServiceInstance | null,
  instanceRevision: 0,
  selectedPlatformClientId: null as string | null,
  selectedPlatformClient: null as PlatformClient | null,
  platformRevision: 0,
  selectedWorkflowId: null as string | null,
  selectedWorkflow: null as WorkflowDefinition | null,
  workflowRevision: 0,
  selectedHttpClientId: null as string | null,
  selectedHttpClient: null as HttpClientInstance | null,
  httpClientRevision: 0,
  selectedKubernetesClientId: null as string | null,
  selectedKubernetesClient: null as KubernetesClientInstance | null,
  kubernetesClientRevision: 0,
  selectedGitClientId: null as string | null,
  selectedGitClient: null as GitClientInstance | null,
  gitClientRevision: 0,
  selectedTemplateId: null as string | null,
  selectedTemplate: null as Template | null,
  templateRevision: 0,
});

export function selectCatalog(catalogId: string | null, catalog: AdminCatalog | null = null) {
  workspace.selectedCatalogId = catalogId;
  workspace.selectedCatalog = catalogId ? catalog : null;
  workspace.selectedOfferingId = null;
  workspace.selectedOffering = null;
  workspace.selectedPlanId = null;
  workspace.selectedPlanOfferingId = null;
  workspace.selectedPlan = null;
  workspace.selectedInstanceId = null;
  workspace.selectedInstance = null;
  if (
    !catalogId
    || (workspace.selectedPlatformClient
      && workspace.selectedPlatformClient.catalogId !== catalogId)
  ) {
    workspace.selectedPlatformClientId = null;
    workspace.selectedPlatformClient = null;
  }
}

export function selectOffering(
  offeringId: string | null,
  offering: ServiceOffering | null = null,
) {
  workspace.selectedOfferingId = offeringId;
  workspace.selectedOffering = offeringId ? offering : null;
  workspace.selectedPlanId = null;
  workspace.selectedPlanOfferingId = null;
  workspace.selectedPlan = null;
  if (!offeringId || workspace.selectedInstance?.serviceId !== offeringId) {
    workspace.selectedInstanceId = null;
    workspace.selectedInstance = null;
  }
}

export function selectPlan(
  planId: string | null,
  offeringId: string | null = null,
  plan: PlanDetail | null = null,
) {
  workspace.selectedPlanId = planId;
  workspace.selectedPlanOfferingId = planId ? offeringId : null;
  workspace.selectedPlan = planId ? plan : null;
  if (offeringId) {
    workspace.selectedOfferingId = offeringId;
    if (workspace.selectedOffering?.id !== offeringId) {
      workspace.selectedOffering = null;
    }
  }
  if (
    !planId
    || workspace.selectedInstance?.planId !== planId
    || (offeringId && workspace.selectedInstance?.serviceId !== offeringId)
  ) {
    workspace.selectedInstanceId = null;
    workspace.selectedInstance = null;
  }
}

export function selectInstance(
  instanceId: string | null,
  instance: ServiceInstance | null = null,
) {
  workspace.selectedInstanceId = instanceId;
  workspace.selectedInstance = instanceId ? instance : null;
}

/**
 * Select an instance and align catalog / offering / plan selection to its service+plan.
 */
export function selectInstanceContext(
  instance: ServiceInstance | null,
  catalogs: AdminCatalog[],
): {
  catalog: AdminCatalog | null;
  offering: ServiceOffering | null;
  plan: PlanDetail | null;
} {
  if (!instance) {
    selectInstance(null);
    return { catalog: null, offering: null, plan: null };
  }

  let catalog: AdminCatalog | null = null;
  let offering: ServiceOffering | null = null;
  let plan: PlanDetail | null = null;

  for (const candidate of catalogs) {
    const foundOffering = candidate.offerings.find((item) => item.id === instance.serviceId);
    if (!foundOffering) continue;
    catalog = candidate;
    offering = foundOffering;
    const foundPlan = foundOffering.plans.find((item) => item.id === instance.planId);
    if (foundPlan) {
      plan = {
        ...foundPlan,
        serviceId: foundOffering.id,
        serviceName: foundOffering.name,
      };
    }
    break;
  }

  workspace.selectedCatalogId = catalog?.id ?? null;
  workspace.selectedCatalog = catalog;
  workspace.selectedOfferingId = offering?.id ?? null;
  workspace.selectedOffering = offering;
  workspace.selectedPlanId = plan?.id ?? null;
  workspace.selectedPlanOfferingId = plan && offering ? offering.id : null;
  workspace.selectedPlan = plan;
  workspace.selectedInstanceId = instance.id;
  workspace.selectedInstance = instance;

  return { catalog, offering, plan };
}

export function selectPlatformClient(
  platformClientId: string | null,
  platformClient: PlatformClient | null = null,
) {
  workspace.selectedPlatformClientId = platformClientId;
  workspace.selectedPlatformClient = platformClientId ? platformClient : null;
}

export function selectWorkflow(
  workflowId: string | null,
  workflow: WorkflowDefinition | null = null,
) {
  workspace.selectedWorkflowId = workflowId;
  workspace.selectedWorkflow = workflowId ? workflow : null;
}

export function selectHttpClient(
  httpClientId: string | null,
  httpClient: HttpClientInstance | null = null,
) {
  workspace.selectedHttpClientId = httpClientId;
  workspace.selectedHttpClient = httpClientId ? httpClient : null;
}

export function notifyCatalogsChanged() {
  workspace.catalogRevision++;
}

export function notifyOfferingsChanged() {
  workspace.offeringRevision++;
}

export function notifyInstancesChanged() {
  workspace.instanceRevision++;
}

export function notifyPlatformsChanged() {
  workspace.platformRevision++;
}

export function notifyWorkflowsChanged() {
  workspace.workflowRevision++;
}

export function notifyHttpClientsChanged() {
  workspace.httpClientRevision++;
}

export function selectKubernetesClient(
  kubernetesClientId: string | null,
  kubernetesClient: KubernetesClientInstance | null = null,
) {
  workspace.selectedKubernetesClientId = kubernetesClientId;
  workspace.selectedKubernetesClient = kubernetesClientId ? kubernetesClient : null;
}

export function notifyKubernetesClientsChanged() {
  workspace.kubernetesClientRevision++;
}

export function selectGitClient(
  gitClientId: string | null,
  gitClient: GitClientInstance | null = null,
) {
  workspace.selectedGitClientId = gitClientId;
  workspace.selectedGitClient = gitClientId ? gitClient : null;
}

export function notifyGitClientsChanged() {
  workspace.gitClientRevision++;
}

export function selectTemplate(
  templateId: string | null,
  template: Template | null = null,
) {
  workspace.selectedTemplateId = templateId;
  workspace.selectedTemplate = templateId ? template : null;
}

export function notifyTemplatesChanged() {
  workspace.templateRevision++;
}
