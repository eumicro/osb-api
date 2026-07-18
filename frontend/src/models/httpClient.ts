export type HttpClientAuthType = "NONE" | "BASIC" | "BEARER" | "CLIENT_CREDENTIALS";

export interface HttpClientInstance {
  id: string;
  name: string;
  description: string;
  baseUrl: string;
  authType: HttpClientAuthType;
  username: string;
  secretConfigured: boolean;
  oauthClientId: string;
  oauthClientSecretConfigured: boolean;
  wellKnownUrl: string;
  timeoutSeconds: number;
  enabled: boolean;
}

export interface CreateHttpClientInstanceRequest {
  name: string;
  description: string;
  baseUrl: string;
  authType: HttpClientAuthType;
  username: string;
  secret: string;
  oauthClientId: string;
  oauthClientSecret: string;
  wellKnownUrl: string;
  timeoutSeconds: number;
  enabled: boolean;
}

export interface UpdateHttpClientInstanceRequest {
  name: string;
  description: string;
  baseUrl: string;
  authType: HttpClientAuthType;
  username: string;
  secret: string;
  oauthClientId: string;
  oauthClientSecret: string;
  wellKnownUrl: string;
  timeoutSeconds: number;
  enabled: boolean;
}

export const HTTP_CLIENT_AUTH_TYPES: readonly HttpClientAuthType[] = [
  "NONE",
  "BASIC",
  "BEARER",
  "CLIENT_CREDENTIALS",
];
