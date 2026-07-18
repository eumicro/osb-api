-- Realtest: Keycloak realm-admin parameters + Redis Commander UI template.
-- Dashboard URLs are set in AdminStore after successful provision (not in SQL).

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
    { "type": "Control", "scope": "#/properties/adminPassword" }
  ]
}'::jsonb
WHERE id = 'keycloak-realm-default';

UPDATE templates
SET description = 'POST Admin API create realm osb-<instanceId> with realm-admin user from parameters.',
    content = E'{"method":"POST","path":"/admin/realms","body":{"realm":"osb-${instanceId}","enabled":true,"displayName":"${parameters.displayName}","attributes":{"osb.serviceId":"${serviceId}","osb.planId":"${planId}","osb.instanceId":"${instanceId}"},"users":[{"username":"${parameters.adminUsername}","enabled":true,"emailVerified":true,"credentials":[{"type":"password","value":"${parameters.adminPassword}","temporary":false}],"clientRoles":{"realm-management":["realm-admin"]}}]}}\n'
WHERE id = 'tpl-kc-realm-create';

UPDATE templates
SET description = 'Redis + Redis Commander UI; applied into namespace = instanceId. UI: kubectl -n <instanceId> port-forward svc/redis-ui 8081:8081',
    content = E'apiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: redis\n  labels:\n    app: redis\n    osb-instance-id: ${instanceId}\nspec:\n  replicas: 1\n  selector:\n    matchLabels:\n      app: redis\n      osb-instance-id: ${instanceId}\n  template:\n    metadata:\n      labels:\n        app: redis\n        osb-instance-id: ${instanceId}\n    spec:\n      containers:\n        - name: redis\n          image: redis:7-alpine\n          ports:\n            - containerPort: 6379\n          resources:\n            limits:\n              memory: ${parameters.memory_mb}Mi\n---\napiVersion: v1\nkind: Service\nmetadata:\n  name: redis\n  labels:\n    app: redis\n    osb-instance-id: ${instanceId}\nspec:\n  selector:\n    app: redis\n    osb-instance-id: ${instanceId}\n  ports:\n    - port: 6379\n      targetPort: 6379\n---\napiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: redis-ui\n  labels:\n    app: redis-ui\n    osb-instance-id: ${instanceId}\nspec:\n  replicas: 1\n  selector:\n    matchLabels:\n      app: redis-ui\n      osb-instance-id: ${instanceId}\n  template:\n    metadata:\n      labels:\n        app: redis-ui\n        osb-instance-id: ${instanceId}\n    spec:\n      containers:\n        - name: redis-commander\n          image: rediscommander/redis-commander:latest\n          env:\n            - name: REDIS_HOSTS\n              value: local:redis:6379\n          ports:\n            - containerPort: 8081\n---\napiVersion: v1\nkind: Service\nmetadata:\n  name: redis-ui\n  labels:\n    app: redis-ui\n    osb-instance-id: ${instanceId}\nspec:\n  selector:\n    app: redis-ui\n    osb-instance-id: ${instanceId}\n  ports:\n    - port: 8081\n      targetPort: 8081\n'
WHERE id = 'tpl-redis-cache';
