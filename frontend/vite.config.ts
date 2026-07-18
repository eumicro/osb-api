import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";

/**
 * Dev: Quinoa (osb-bff :8081) starts this Vite server and proxies it.
 * Do not start a second `npm run dev` on the same port.
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    port: 5173,
    strictPort: true,
    host: "127.0.0.1",
  },
  build: {
    outDir: "dist",
  },
  optimizeDeps: {
    include: ["monaco-editor", "@guolao/vue-monaco-editor"],
  },
});
