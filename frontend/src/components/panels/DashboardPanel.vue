<script setup lang="ts">
import { onMounted } from "vue";
import { useDashboard } from "../../controllers/useDashboard";
import { pluginRegistry } from "../../plugins/registry";
import ErrorMessage from "../molecules/ErrorMessage.vue";
import PanelToolbar from "../molecules/PanelToolbar.vue";
import ReloadButton from "../molecules/ReloadButton.vue";
import DashboardOverview from "../organisms/DashboardOverview.vue";

const { info, catalog, loading, error, load } = useDashboard();
const pluginCount = pluginRegistry.list().length;

onMounted(() => {
  void load();
});
</script>

<template>
  <div class="panel-body">
    <PanelToolbar>
      <template #end>
        <ReloadButton :loading="loading" @click="load" />
      </template>
    </PanelToolbar>
    <ErrorMessage :message="error" />
    <DashboardOverview :info="info" :catalog="catalog" :plugin-count="pluginCount" />
  </div>
</template>
