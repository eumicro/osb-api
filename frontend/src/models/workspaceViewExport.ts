import type { WorkspaceView, WorkspaceViewsState } from "./workspaceView";

export const WORKSPACE_VIEWS_FORMAT = "osb-workspace-views";
export const WORKSPACE_VIEWS_FORMAT_VERSION = 1;

/** Portable JSON document for importing/exporting workspace views. */
export interface WorkspaceViewsExportDocument {
  format: typeof WORKSPACE_VIEWS_FORMAT;
  version: typeof WORKSPACE_VIEWS_FORMAT_VERSION;
  exportedAt: string;
  activeViewId: string;
  views: WorkspaceView[];
}

export function toExportDocument(state: WorkspaceViewsState): WorkspaceViewsExportDocument {
  return {
    format: WORKSPACE_VIEWS_FORMAT,
    version: WORKSPACE_VIEWS_FORMAT_VERSION,
    exportedAt: new Date().toISOString(),
    activeViewId: state.activeViewId,
    views: state.views.map((view) => ({
      id: view.id,
      name: view.name,
      layout: view.layout ?? {},
    })),
  };
}

export function parseExportDocument(raw: unknown): WorkspaceViewsState {
  if (!raw || typeof raw !== "object") {
    throw new Error("invalid document");
  }
  const doc = raw as Partial<WorkspaceViewsExportDocument> & {
    views?: unknown;
    activeViewId?: unknown;
  };

  // Accept bare WorkspaceViewsState (activeViewId + views) as well as full export docs.
  const viewsRaw = doc.views;
  if (!Array.isArray(viewsRaw) || viewsRaw.length === 0) {
    throw new Error("views missing");
  }

  const views: WorkspaceView[] = viewsRaw.map((entry, index) => {
    if (!entry || typeof entry !== "object") {
      throw new Error(`view[${index}] invalid`);
    }
    const view = entry as Partial<WorkspaceView>;
    if (typeof view.name !== "string" || view.name.trim() === "") {
      throw new Error(`view[${index}] name missing`);
    }
    if (view.layout == null || typeof view.layout !== "object") {
      throw new Error(`view[${index}] layout missing`);
    }
    const id =
      typeof view.id === "string" && view.id.trim() !== ""
        ? view.id.trim()
        : crypto.randomUUID();
    return {
      id,
      name: view.name.trim(),
      layout: view.layout,
    };
  });

  if (doc.format != null && doc.format !== WORKSPACE_VIEWS_FORMAT) {
    throw new Error("unsupported format");
  }
  if (doc.version != null && doc.version !== WORKSPACE_VIEWS_FORMAT_VERSION) {
    throw new Error("unsupported version");
  }

  const activeViewId =
    typeof doc.activeViewId === "string" && views.some((view) => view.id === doc.activeViewId)
      ? doc.activeViewId
      : views[0].id;

  return { activeViewId, views };
}

export function exportFilename(activeViewName: string | null | undefined): string {
  const stamp = new Date().toISOString().slice(0, 10);
  const slug = (activeViewName ?? "views")
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "")
    .slice(0, 40);
  return `osb-views-${slug || "views"}-${stamp}.json`;
}
