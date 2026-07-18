<script setup lang="ts">
defineProps<{
  src: string;
}>();
</script>

<template>
  <div class="n8n-embed">
    <iframe
      class="n8n-frame"
      :src="src"
      title="n8n"
      referrerpolicy="no-referrer-when-downgrade"
      allow="clipboard-read; clipboard-write; fullscreen"
    />
  </div>
</template>

<style scoped>
.n8n-embed {
  flex: 1;
  min-height: 0;
  border: 1px solid var(--border);
  border-radius: 0.35rem;
  overflow: hidden;
  background: var(--bg);
}

.n8n-frame {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
  background: var(--surface);
}
</style>
