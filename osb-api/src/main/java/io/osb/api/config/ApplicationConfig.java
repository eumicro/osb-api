package io.osb.api.config;

import io.osb.application.catalog.DeleteCatalogUseCase;
import io.osb.application.catalog.DeleteOfferingUseCase;
import io.osb.application.catalog.DeletePlanUseCase;
import io.osb.application.catalog.GetCatalogByIdUseCase;
import io.osb.application.catalog.GetCatalogForPlatformUseCase;
import io.osb.application.catalog.GetCatalogUseCase;
import io.osb.application.catalog.GetOfferingUseCase;
import io.osb.application.catalog.GetPlanUseCase;
import io.osb.application.catalog.ListCatalogsUseCase;
import io.osb.application.catalog.MovePlanUseCase;
import io.osb.application.catalog.SaveCatalogUseCase;
import io.osb.application.catalog.SaveOfferingUseCase;
import io.osb.application.catalog.SavePlanUseCase;
import io.osb.application.gitclients.DeleteGitClientInstanceUseCase;
import io.osb.application.gitclients.GetGitClientInstanceUseCase;
import io.osb.application.gitclients.ListGitClientInstancesUseCase;
import io.osb.application.gitclients.SaveGitClientInstanceUseCase;
import io.osb.application.httpclients.DeleteHttpClientInstanceUseCase;
import io.osb.application.httpclients.GetHttpClientInstanceUseCase;
import io.osb.application.httpclients.ListHttpClientInstancesUseCase;
import io.osb.application.httpclients.SaveHttpClientInstanceUseCase;
import io.osb.application.kubernetesclients.DeleteKubernetesClientInstanceUseCase;
import io.osb.application.kubernetesclients.GetKubernetesClientInstanceUseCase;
import io.osb.application.kubernetesclients.ListKubernetesClientInstancesUseCase;
import io.osb.application.kubernetesclients.SaveKubernetesClientInstanceUseCase;
import io.osb.application.platforms.DeletePlatformClientUseCase;
import io.osb.application.platforms.GetPlatformClientUseCase;
import io.osb.application.platforms.ListPlatformClientsUseCase;
import io.osb.application.platforms.SavePlatformClientUseCase;
import io.osb.application.templates.DeleteTemplateUseCase;
import io.osb.application.templates.GetTemplateUseCase;
import io.osb.application.templates.ListTemplatesUseCase;
import io.osb.application.templates.SaveTemplateUseCase;
import io.osb.application.workflows.DeleteWorkflowUseCase;
import io.osb.application.workflows.GetWorkflowUseCase;
import io.osb.application.workflows.ListWorkflowsUseCase;
import io.osb.application.workflows.SaveWorkflowUseCase;
import io.osb.domain.catalog.CatalogRepository;
import io.osb.domain.gitclients.GitClientInstanceRepository;
import io.osb.domain.httpclients.HttpClientInstanceRepository;
import io.osb.domain.kubernetesclients.KubernetesClientInstanceRepository;
import io.osb.domain.platforms.PlatformClientRepository;
import io.osb.domain.templates.TemplateRepository;
import io.osb.domain.workflows.WorkflowDefinitionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@ApplicationScoped
public class ApplicationConfig {

    @Produces
    @Singleton
    GetCatalogUseCase getCatalogUseCase(CatalogRepository catalogRepository) {
        return new GetCatalogUseCase(catalogRepository);
    }

    @Produces
    @Singleton
    GetCatalogForPlatformUseCase getCatalogForPlatformUseCase(
            PlatformClientRepository platformClientRepository,
            CatalogRepository catalogRepository) {
        return new GetCatalogForPlatformUseCase(platformClientRepository, catalogRepository);
    }

    @Produces
    @Singleton
    ListCatalogsUseCase listCatalogsUseCase(CatalogRepository catalogRepository) {
        return new ListCatalogsUseCase(catalogRepository);
    }

    @Produces
    @Singleton
    GetCatalogByIdUseCase getCatalogByIdUseCase(CatalogRepository catalogRepository) {
        return new GetCatalogByIdUseCase(catalogRepository);
    }

    @Produces
    @Singleton
    SaveCatalogUseCase saveCatalogUseCase(CatalogRepository catalogRepository) {
        return new SaveCatalogUseCase(catalogRepository);
    }

    @Produces
    @Singleton
    DeleteCatalogUseCase deleteCatalogUseCase(CatalogRepository catalogRepository) {
        return new DeleteCatalogUseCase(catalogRepository);
    }

    @Produces
    @Singleton
    GetOfferingUseCase getOfferingUseCase(CatalogRepository catalogRepository) {
        return new GetOfferingUseCase(catalogRepository);
    }

    @Produces
    @Singleton
    SaveOfferingUseCase saveOfferingUseCase(CatalogRepository catalogRepository) {
        return new SaveOfferingUseCase(catalogRepository);
    }

    @Produces
    @Singleton
    DeleteOfferingUseCase deleteOfferingUseCase(CatalogRepository catalogRepository) {
        return new DeleteOfferingUseCase(catalogRepository);
    }

    @Produces
    @Singleton
    GetPlanUseCase getPlanUseCase(CatalogRepository catalogRepository) {
        return new GetPlanUseCase(catalogRepository);
    }

    @Produces
    @Singleton
    SavePlanUseCase savePlanUseCase(CatalogRepository catalogRepository) {
        return new SavePlanUseCase(catalogRepository);
    }

    @Produces
    @Singleton
    DeletePlanUseCase deletePlanUseCase(CatalogRepository catalogRepository) {
        return new DeletePlanUseCase(catalogRepository);
    }

    @Produces
    @Singleton
    MovePlanUseCase movePlanUseCase(CatalogRepository catalogRepository) {
        return new MovePlanUseCase(catalogRepository);
    }

    @Produces
    @Singleton
    ListPlatformClientsUseCase listPlatformClientsUseCase(
            PlatformClientRepository platformClientRepository) {
        return new ListPlatformClientsUseCase(platformClientRepository);
    }

    @Produces
    @Singleton
    GetPlatformClientUseCase getPlatformClientUseCase(
            PlatformClientRepository platformClientRepository) {
        return new GetPlatformClientUseCase(platformClientRepository);
    }

    @Produces
    @Singleton
    SavePlatformClientUseCase savePlatformClientUseCase(
            PlatformClientRepository platformClientRepository,
            CatalogRepository catalogRepository) {
        return new SavePlatformClientUseCase(platformClientRepository, catalogRepository);
    }

    @Produces
    @Singleton
    DeletePlatformClientUseCase deletePlatformClientUseCase(
            PlatformClientRepository platformClientRepository) {
        return new DeletePlatformClientUseCase(platformClientRepository);
    }

    @Produces
    @Singleton
    ListWorkflowsUseCase listWorkflowsUseCase(
            WorkflowDefinitionRepository workflowDefinitionRepository) {
        return new ListWorkflowsUseCase(workflowDefinitionRepository);
    }

    @Produces
    @Singleton
    GetWorkflowUseCase getWorkflowUseCase(
            WorkflowDefinitionRepository workflowDefinitionRepository) {
        return new GetWorkflowUseCase(workflowDefinitionRepository);
    }

    @Produces
    @Singleton
    SaveWorkflowUseCase saveWorkflowUseCase(
            WorkflowDefinitionRepository workflowDefinitionRepository,
            HttpClientInstanceRepository httpClientInstanceRepository,
            KubernetesClientInstanceRepository kubernetesClientInstanceRepository,
            GitClientInstanceRepository gitClientInstanceRepository,
            TemplateRepository templateRepository) {
        return new SaveWorkflowUseCase(
                workflowDefinitionRepository,
                httpClientInstanceRepository,
                kubernetesClientInstanceRepository,
                gitClientInstanceRepository,
                templateRepository);
    }

    @Produces
    @Singleton
    ListTemplatesUseCase listTemplatesUseCase(TemplateRepository templateRepository) {
        return new ListTemplatesUseCase(templateRepository);
    }

    @Produces
    @Singleton
    GetTemplateUseCase getTemplateUseCase(TemplateRepository templateRepository) {
        return new GetTemplateUseCase(templateRepository);
    }

    @Produces
    @Singleton
    SaveTemplateUseCase saveTemplateUseCase(TemplateRepository templateRepository) {
        return new SaveTemplateUseCase(templateRepository);
    }

    @Produces
    @Singleton
    DeleteTemplateUseCase deleteTemplateUseCase(TemplateRepository templateRepository) {
        return new DeleteTemplateUseCase(templateRepository);
    }

    @Produces
    @Singleton
    DeleteWorkflowUseCase deleteWorkflowUseCase(
            WorkflowDefinitionRepository workflowDefinitionRepository) {
        return new DeleteWorkflowUseCase(workflowDefinitionRepository);
    }

    @Produces
    @Singleton
    ListHttpClientInstancesUseCase listHttpClientInstancesUseCase(
            HttpClientInstanceRepository httpClientInstanceRepository) {
        return new ListHttpClientInstancesUseCase(httpClientInstanceRepository);
    }

    @Produces
    @Singleton
    GetHttpClientInstanceUseCase getHttpClientInstanceUseCase(
            HttpClientInstanceRepository httpClientInstanceRepository) {
        return new GetHttpClientInstanceUseCase(httpClientInstanceRepository);
    }

    @Produces
    @Singleton
    SaveHttpClientInstanceUseCase saveHttpClientInstanceUseCase(
            HttpClientInstanceRepository httpClientInstanceRepository) {
        return new SaveHttpClientInstanceUseCase(httpClientInstanceRepository);
    }

    @Produces
    @Singleton
    DeleteHttpClientInstanceUseCase deleteHttpClientInstanceUseCase(
            HttpClientInstanceRepository httpClientInstanceRepository) {
        return new DeleteHttpClientInstanceUseCase(httpClientInstanceRepository);
    }

    @Produces
    @Singleton
    ListKubernetesClientInstancesUseCase listKubernetesClientInstancesUseCase(
            KubernetesClientInstanceRepository kubernetesClientInstanceRepository) {
        return new ListKubernetesClientInstancesUseCase(kubernetesClientInstanceRepository);
    }

    @Produces
    @Singleton
    GetKubernetesClientInstanceUseCase getKubernetesClientInstanceUseCase(
            KubernetesClientInstanceRepository kubernetesClientInstanceRepository) {
        return new GetKubernetesClientInstanceUseCase(kubernetesClientInstanceRepository);
    }

    @Produces
    @Singleton
    SaveKubernetesClientInstanceUseCase saveKubernetesClientInstanceUseCase(
            KubernetesClientInstanceRepository kubernetesClientInstanceRepository) {
        return new SaveKubernetesClientInstanceUseCase(kubernetesClientInstanceRepository);
    }

    @Produces
    @Singleton
    DeleteKubernetesClientInstanceUseCase deleteKubernetesClientInstanceUseCase(
            KubernetesClientInstanceRepository kubernetesClientInstanceRepository) {
        return new DeleteKubernetesClientInstanceUseCase(kubernetesClientInstanceRepository);
    }

    @Produces
    @Singleton
    ListGitClientInstancesUseCase listGitClientInstancesUseCase(
            GitClientInstanceRepository gitClientInstanceRepository) {
        return new ListGitClientInstancesUseCase(gitClientInstanceRepository);
    }

    @Produces
    @Singleton
    GetGitClientInstanceUseCase getGitClientInstanceUseCase(
            GitClientInstanceRepository gitClientInstanceRepository) {
        return new GetGitClientInstanceUseCase(gitClientInstanceRepository);
    }

    @Produces
    @Singleton
    SaveGitClientInstanceUseCase saveGitClientInstanceUseCase(
            GitClientInstanceRepository gitClientInstanceRepository) {
        return new SaveGitClientInstanceUseCase(gitClientInstanceRepository);
    }

    @Produces
    @Singleton
    DeleteGitClientInstanceUseCase deleteGitClientInstanceUseCase(
            GitClientInstanceRepository gitClientInstanceRepository) {
        return new DeleteGitClientInstanceUseCase(gitClientInstanceRepository);
    }
}
