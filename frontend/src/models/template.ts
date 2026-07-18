export type TemplateKind =
  | "KUBERNETES_RESOURCE"
  | "HTTP_REQUEST"
  | "GIT_COMMAND"
  | "TEXT";

export interface Template {
  id: string;
  name: string;
  description: string;
  kind: TemplateKind;
  content: string;
  enabled: boolean;
}

export interface CreateTemplateRequest {
  name: string;
  description: string;
  kind: TemplateKind;
  content: string;
  enabled: boolean;
}

export interface UpdateTemplateRequest {
  name: string;
  description: string;
  kind: TemplateKind;
  content: string;
  enabled: boolean;
}

export const TEMPLATE_KINDS: readonly TemplateKind[] = [
  "KUBERNETES_RESOURCE",
  "HTTP_REQUEST",
  "GIT_COMMAND",
  "TEXT",
];
