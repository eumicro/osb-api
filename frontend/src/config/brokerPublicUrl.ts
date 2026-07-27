/** Public OSB broker base URL for platform operators (not the Admin BFF). */
export function brokerPublicUrl(): string {
  const raw = import.meta.env.VITE_OSB_BROKER_PUBLIC_URL || "http://localhost:8080";
  return String(raw).trim().replace(/\/+$/, "") || "http://localhost:8080";
}

export function brokerCatalogUrl(): string {
  return `${brokerPublicUrl()}/v2/catalog`;
}
