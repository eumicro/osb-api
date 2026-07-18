import { createApp } from "vue";
import { install as installJsonSchemaEditor } from "@jsonschema-editor/vue";
import { registerDefaultVueExtensions } from "@jsonschema-editor/vue-extensions";
import App from "./App.vue";
import CatalogPanel from "./components/panels/CatalogPanel.vue";
import CatalogsPanel from "./components/panels/CatalogsPanel.vue";
import DashboardPanel from "./components/panels/DashboardPanel.vue";
import InstancePanel from "./components/panels/InstancePanel.vue";
import InstancesPanel from "./components/panels/InstancesPanel.vue";
import NavigationPanel from "./components/panels/NavigationPanel.vue";
import OfferingPanel from "./components/panels/OfferingPanel.vue";
import OfferingsPanel from "./components/panels/OfferingsPanel.vue";
import PlanPanel from "./components/panels/PlanPanel.vue";
import PlansPanel from "./components/panels/PlansPanel.vue";
import GitClientPanel from "./components/panels/GitClientPanel.vue";
import GitClientsPanel from "./components/panels/GitClientsPanel.vue";
import HttpClientPanel from "./components/panels/HttpClientPanel.vue";
import HttpClientsPanel from "./components/panels/HttpClientsPanel.vue";
import KubernetesClientPanel from "./components/panels/KubernetesClientPanel.vue";
import KubernetesClientsPanel from "./components/panels/KubernetesClientsPanel.vue";
import PlatformPanel from "./components/panels/PlatformPanel.vue";
import PlatformsPanel from "./components/panels/PlatformsPanel.vue";
import TemplatePanel from "./components/panels/TemplatePanel.vue";
import TemplatesPanel from "./components/panels/TemplatesPanel.vue";
import WorkflowPanel from "./components/panels/WorkflowPanel.vue";
import WorkflowsPanel from "./components/panels/WorkflowsPanel.vue";
import { applyDocumentLocale, i18n, type AppLocale } from "./i18n";
import { setupMonaco } from "./monaco";
import { catalogPlugin } from "./plugins/catalogPlugin";
import { pluginRegistry } from "./plugins/registry";
import { applyTheme, detectSystemTheme } from "./theme";
import "@jsonschema-editor/vue/style.css";
import "dockview-vue/dist/styles/dockview.css";
import "./styles.css";

pluginRegistry.register(catalogPlugin);
registerDefaultVueExtensions();
setupMonaco();

const app = createApp(App);
app.use(i18n);
installJsonSchemaEditor(app);

applyTheme(detectSystemTheme());
applyDocumentLocale(i18n.global.locale.value as AppLocale);

// Dockview instanziiert Panels ueber ihren global registrierten Komponentennamen.
app.component("NavigationPanel", NavigationPanel);
app.component("DashboardPanel", DashboardPanel);
app.component("CatalogsPanel", CatalogsPanel);
app.component("CatalogPanel", CatalogPanel);
app.component("OfferingsPanel", OfferingsPanel);
app.component("OfferingPanel", OfferingPanel);
app.component("PlansPanel", PlansPanel);
app.component("PlanPanel", PlanPanel);
app.component("InstancesPanel", InstancesPanel);
app.component("InstancePanel", InstancePanel);
app.component("PlatformsPanel", PlatformsPanel);
app.component("PlatformPanel", PlatformPanel);
app.component("WorkflowsPanel", WorkflowsPanel);
app.component("WorkflowPanel", WorkflowPanel);
app.component("HttpClientsPanel", HttpClientsPanel);
app.component("HttpClientPanel", HttpClientPanel);
app.component("KubernetesClientsPanel", KubernetesClientsPanel);
app.component("KubernetesClientPanel", KubernetesClientPanel);
app.component("GitClientsPanel", GitClientsPanel);
app.component("GitClientPanel", GitClientPanel);
app.component("TemplatesPanel", TemplatesPanel);
app.component("TemplatePanel", TemplatePanel);

void pluginRegistry.setupAll().then(() => {
  app.mount("#app");
});
