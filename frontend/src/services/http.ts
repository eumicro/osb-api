export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly body?: unknown,
  ) {
    super(message);
  }
}

/** Full-page OIDC login when the BFF session is missing/expired. */
function redirectToLogin(): never {
  window.location.assign("/");
  throw new ApiError(401, "Authentication required");
}

async function request<T>(
  method: string,
  path: string,
  body?: unknown,
  headers?: Record<string, string>,
): Promise<T> {
  const response = await fetch(path, {
    method,
    // Do not follow OIDC 302 to Keycloak — that triggers CORS on the auth endpoint.
    redirect: "manual",
    headers: {
      Accept: "application/json",
      // Lets Quarkus treat this as a JS call (499 when java-script-auto-redirect=false).
      "X-Requested-With": "XMLHttpRequest",
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  // opaque redirect / 302 / 499 → browser must navigate for OIDC code flow
  if (
    response.type === "opaqueredirect" ||
    response.status === 0 ||
    response.status === 302 ||
    response.status === 499
  ) {
    redirectToLogin();
  }

  if (!response.ok) {
    if (response.status === 401) {
      redirectToLogin();
    }
    let payload: unknown;
    let message = `${method} ${path} failed (${response.status})`;
    try {
      payload = await response.json();
      const m = (payload as { message?: string; description?: string }).message
        ?? (payload as { description?: string }).description;
      if (m) message = m;
    } catch {
      /* response without JSON body */
    }
    throw new ApiError(response.status, message, payload);
  }

  if (response.status === 204 || response.headers.get("content-length") === "0") {
    return undefined as T;
  }

  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const http = {
  get: <T>(path: string, headers?: Record<string, string>) =>
    request<T>("GET", path, undefined, headers),
  post: <T>(path: string, body?: unknown) => request<T>("POST", path, body),
  put: <T>(path: string, body?: unknown) => request<T>("PUT", path, body),
  delete: <T>(path: string, body?: unknown) => request<T>("DELETE", path, body),
};
