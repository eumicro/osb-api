export interface ServicePlan {
  id: string;
  name: string;
  description: string;
  free: boolean;
  bindable: boolean;
  /** OSB plan schemas (service_instance.create.parameters, …). */
  schemas?: Record<string, unknown> | null;
  /** Admin UI schema for provision-parameter forms. */
  parametersUiSchema?: Record<string, unknown> | null;
}

export interface ServiceOffering {
  id: string;
  name: string;
  description: string;
  bindable: boolean;
  plans: ServicePlan[];
}

/** OSB broker catalog projection (`GET /v2/catalog`). */
export interface Catalog {
  services: ServiceOffering[];
}

/** Admin catalog aggregate (multiple catalogs, assigned per platform). */
export interface AdminCatalog {
  id: string;
  name: string;
  description: string;
  offerings: ServiceOffering[];
}
