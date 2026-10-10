-- Core Domain Service offering (Homelab / OSB realtest). Idempotent for upgrades.

INSERT INTO service_offerings (id, catalog_id, name, description, bindable, sort_order)
SELECT
    'domain-service',
    'emergence-platformb',
    'Core Domain Service',
    'Provision creates Keycloak realm osb-<instanceId> and deploys Postgres + OpenFGA + domain-api + domain-bff into namespace <instanceId>.
UI: https://domain-<instanceId>.cloudapplication.lan/',
    FALSE,
    0
WHERE EXISTS (SELECT 1 FROM catalogs WHERE id = 'emergence-platformb')
  AND NOT EXISTS (SELECT 1 FROM service_offerings WHERE id = 'domain-service');

INSERT INTO service_plans (id, offering_id, name, description, free, bindable, sort_order, schemas, parameters_ui_schema)
SELECT
    'domain-service-free',
    'domain-service',
    'Domain Service FREE',
    'Domain stack from GHCR; OIDC realm osb-<instanceId> on central Keycloak',
    TRUE,
    FALSE,
    0,
    '{
      "service_instance": {
        "create": {
          "parameters": {
            "type": "object",
            "title": "Core Domain Service parameters",
            "required": ["image_tag", "oidc_client_id"],
            "properties": {
              "image_tag": {
                "type": "string",
                "title": "Image tag (GHCR domain-api / domain-bff)",
                "default": "0.1.3",
                "minLength": 1
              },
              "oidc_client_id": {
                "type": "string",
                "title": "OIDC client id (instance realm)",
                "default": "domain-bff",
                "minLength": 1
              }
            }
          }
        }
      }
    }'::jsonb,
    '{
      "type": "VerticalLayout",
      "elements": [
        { "type": "Control", "scope": "#/properties/image_tag" },
        { "type": "Control", "scope": "#/properties/oidc_client_id" }
      ]
    }'::jsonb
WHERE EXISTS (SELECT 1 FROM service_offerings WHERE id = 'domain-service')
  AND NOT EXISTS (SELECT 1 FROM service_plans WHERE id = 'domain-service-free');
