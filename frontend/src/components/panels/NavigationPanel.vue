<script setup lang="ts">
import { useNavigation } from "../../controllers/useNavigation";
import NavigationMenu from "../organisms/NavigationMenu.vue";

const { items, activate } = useNavigation();
</script>

<template>
  <div class="panel-body navigation-panel">
    <p class="muted">{{ $t("navigation.hint") }}</p>
    <NavigationMenu :items="items" @activate="activate" />
  </div>
</template>

<style scoped>
.navigation-panel {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
</style>
