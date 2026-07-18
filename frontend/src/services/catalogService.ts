import type { Catalog } from "../models/catalog";
import { http } from "./http";

/** Demo Basic Auth for dashboard preview of a platform-scoped OSB catalog. */
const DEMO_PLATFORM_AUTH = "Basic " + btoa("cf-broker:secret");

export const catalogService = {
  getCatalog: (username = "cf-broker") =>
    http.get<Catalog>("/v2/catalog", {
      "X-Broker-API-Version": "2.17",
      Authorization:
        username === "cf-broker" ? DEMO_PLATFORM_AUTH : "Basic " + btoa(`${username}:secret`),
    }),
};
