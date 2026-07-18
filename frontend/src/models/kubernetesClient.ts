export type KubernetesClientAuthType = "NONE" | "BASIC" | "BEARER" | "CLIENT_CREDENTIALS";

export interface KubernetesClientInstance {
  id: string;
  name: string;
  description: string;
  apiServerUrl: string;
  defaultNamespace: string;
  authType: KubernetesClientAuthType;
  username: string;
  tokenConfigured: boolean;
  oauthClientId: string;
  oauthClientSecretConfigured: boolean;
  wellKnownUrl: string;
  insecureSkipTlsVerify: boolean;
  timeoutSeconds: number;
  enabled: boolean;
}

export interface CreateKubernetesClientInstanceRequest {
  name: string;
  description: string;
  apiServerUrl: string;
  defaultNamespace: string;
  authType: KubernetesClientAuthType;
  username: string;
  token: string;
  oauthClientId: string;
  oauthClientSecret: string;
  wellKnownUrl: string;
  insecureSkipTlsVerify: boolean;
  timeoutSeconds: number;
  enabled: boolean;
}

export interface UpdateKubernetesClientInstanceRequest {
  name: string;
  description: string;
  apiServerUrl: string;
  defaultNamespace: string;
  authType: KubernetesClientAuthType;
  username: string;
  token: string;
  oauthClientId: string;
  oauthClientSecret: string;
  wellKnownUrl: string;
  insecureSkipTlsVerify: boolean;
  timeoutSeconds: number;
  enabled: boolean;
}

export const KUBERNETES_CLIENT_AUTH_TYPES: readonly KubernetesClientAuthType[] = [
  "NONE",
  "BASIC",
  "BEARER",
  "CLIENT_CREDENTIALS",
];
