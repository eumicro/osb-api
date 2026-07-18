-- Keycloak realm-admin: optional temporary password (UPDATE_PASSWORD on first login).

UPDATE service_plans
SET schemas = '{
  "service_instance": {
    "create": {
      "parameters": {
        "type": "object",
        "title": "Realm parameters",
        "properties": {
          "displayName": {
            "type": "string",
            "title": "Display name",
            "default": "OSB Realm"
          },
          "adminUsername": {
            "type": "string",
            "title": "Realm admin username",
            "minLength": 1,
            "default": "realm-admin"
          },
          "adminPassword": {
            "type": "string",
            "title": "Realm admin password",
            "minLength": 1,
            "default": "realm-admin"
          },
          "adminPasswordTemporary": {
            "type": "boolean",
            "title": "Temporary password (change on first login)",
            "default": true
          }
        },
        "required": ["adminUsername", "adminPassword"]
      }
    }
  }
}'::jsonb,
    parameters_ui_schema = '{
  "type": "VerticalLayout",
  "elements": [
    { "type": "Control", "scope": "#/properties/displayName" },
    { "type": "Control", "scope": "#/properties/adminUsername" },
    { "type": "Control", "scope": "#/properties/adminPassword" },
    { "type": "Control", "scope": "#/properties/adminPasswordTemporary" }
  ]
}'::jsonb
WHERE id = 'keycloak-realm-default';

UPDATE templates
SET content = E'{"method":"POST","path":"/admin/realms","body":{"realm":"osb-${instanceId}","enabled":true,"displayName":"${parameters.displayName}","verifyEmail":false,"loginWithEmailAllowed":true,"registrationAllowed":false,"attributes":{"osb.serviceId":"${serviceId}","osb.planId":"${planId}","osb.instanceId":"${instanceId}"},"users":[{"username":"${parameters.adminUsername}","firstName":"${parameters.adminUsername}","lastName":"Admin","email":"${parameters.adminUsername}@localhost","enabled":true,"emailVerified":true,"credentials":[{"type":"password","value":"${parameters.adminPassword}","temporary":"${parameters.adminPasswordTemporary}"}],"clientRoles":{"realm-management":["realm-admin"]}}]}}\n'
WHERE id = 'tpl-kc-realm-create';
