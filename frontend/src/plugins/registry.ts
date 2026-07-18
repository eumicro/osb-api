import type { OsbPlugin } from "./types";

const plugins: OsbPlugin[] = [];

export const pluginRegistry = {
  register(plugin: OsbPlugin) {
    if (plugins.some((p) => p.id === plugin.id)) {
      throw new Error(`Plugin already registered: ${plugin.id}`);
    }
    plugins.push(plugin);
  },

  list(): readonly OsbPlugin[] {
    return plugins;
  },

  async setupAll() {
    for (const plugin of plugins) {
      await plugin.setup?.();
    }
  },
};
