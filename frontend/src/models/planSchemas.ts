import type { JsonSchemaObject } from "@jsonschema-editor/json-schema";
import type { UiSchemaObject } from "@jsonschema-editor/ui-schema";

/** Empty JSON Schema for plan create-parameters. */
export const EMPTY_PARAMETERS_SCHEMA: JsonSchemaObject = {
  type: "object",
  properties: {},
};

export type OsbPlanSchemas = {
  service_instance?: {
    create?: {
      parameters?: JsonSchemaObject;
    };
    update?: {
      parameters?: JsonSchemaObject;
    };
  };
  service_binding?: {
    create?: {
      parameters?: JsonSchemaObject;
    };
  };
  [key: string]: unknown;
};

export function extractCreateParametersSchema(
  schemas: Record<string, unknown> | null | undefined,
): JsonSchemaObject {
  const nested = (schemas as OsbPlanSchemas | undefined)?.service_instance?.create?.parameters;
  if (nested && typeof nested === "object" && !Array.isArray(nested)) {
    return nested as JsonSchemaObject;
  }
  return { ...EMPTY_PARAMETERS_SCHEMA, properties: {} };
}

export function withCreateParametersSchema(
  schemas: Record<string, unknown> | null | undefined,
  parametersSchema: JsonSchemaObject,
): OsbPlanSchemas {
  const current = (schemas ?? {}) as OsbPlanSchemas;
  const serviceInstance = (current.service_instance ?? {}) as NonNullable<
    OsbPlanSchemas["service_instance"]
  >;
  const create = (serviceInstance.create ?? {}) as NonNullable<
    NonNullable<OsbPlanSchemas["service_instance"]>["create"]
  >;
  return {
    ...current,
    service_instance: {
      ...serviceInstance,
      create: {
        ...create,
        parameters: parametersSchema,
      },
    },
  };
}

export function isUiSchemaObject(value: unknown): value is UiSchemaObject {
  return (
    !!value
    && typeof value === "object"
    && !Array.isArray(value)
    && typeof (value as UiSchemaObject).type === "string"
  );
}
