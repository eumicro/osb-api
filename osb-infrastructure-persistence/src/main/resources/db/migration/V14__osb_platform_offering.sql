-- Offering: provision a nested OSB API + Admin UI stack into Kubernetes
-- (namespace = instanceId). Manifests are applied via the K8s client (same path as redis-cache).
-- Images default to GHCR; override with plan parameter image_tag.

INSERT INTO service_offerings (id, catalog_id, name, description, bindable, sort_order) VALUES
    ('osb-platform', 'k8s-ops', 'osb-platform',
     'Provision deploys Postgres + osb-api + osb-bff (Admin UI, no-auth) into namespace <instanceId>; UI via Ingress osb-ui-<instanceId>.localhost.',
     TRUE, 1);

INSERT INTO service_plans (id, offering_id, name, description, free, bindable, sort_order, schemas, parameters_ui_schema) VALUES
(
    'osb-platform-default', 'osb-platform', 'default',
    'Nested OSB stack from GHCR images',
    TRUE, TRUE, 0,
    '{
      "service_instance": {
        "create": {
          "parameters": {
            "type": "object",
            "title": "OSB platform parameters",
            "properties": {
              "image_tag": {
                "type": "string",
                "title": "Image tag (GHCR)",
                "default": "0.1.0",
                "minLength": 1
              }
            },
            "required": ["image_tag"]
          }
        }
      }
    }'::jsonb,
    '{
      "type": "VerticalLayout",
      "elements": [
        { "type": "Control", "scope": "#/properties/image_tag" }
      ]
    }'::jsonb
);

INSERT INTO templates (id, name, description, kind, content, enabled) VALUES
(
    'tpl-osb-platform',
    'OSB Platform Stack',
    'Postgres + osb-api + osb-bff + Ingress; applied into namespace = instanceId.',
    'KUBERNETES_RESOURCE',
    $tpl$
apiVersion: v1
kind: Secret
metadata:
  name: osb-db
  labels:
    app.kubernetes.io/name: osb-platform
    osb-instance-id: ${instanceId}
type: Opaque
stringData:
  POSTGRES_PASSWORD: osb
  OSB_POSTGRES_PASSWORD: osb
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  labels:
    app: postgres
    osb-instance-id: ${instanceId}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
      osb-instance-id: ${instanceId}
  template:
    metadata:
      labels:
        app: postgres
        osb-instance-id: ${instanceId}
    spec:
      containers:
        - name: postgres
          image: postgres:17-alpine
          ports:
            - containerPort: 5432
          env:
            - name: POSTGRES_DB
              value: osb
            - name: POSTGRES_USER
              value: osb
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: osb-db
                  key: POSTGRES_PASSWORD
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", "osb", "-d", "osb"]
            initialDelaySeconds: 5
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  labels:
    app: postgres
    osb-instance-id: ${instanceId}
spec:
  selector:
    app: postgres
    osb-instance-id: ${instanceId}
  ports:
    - port: 5432
      targetPort: 5432
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: osb-api
  labels:
    app: osb-api
    osb-instance-id: ${instanceId}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: osb-api
      osb-instance-id: ${instanceId}
  template:
    metadata:
      labels:
        app: osb-api
        osb-instance-id: ${instanceId}
    spec:
      containers:
        - name: osb-api
          image: ghcr.io/eumicro/osb-api/osb-api:${parameters.image_tag}
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          env:
            - name: QUARKUS_PROFILE
              value: compose
            - name: OSB_POSTGRES_JDBC_URL
              value: jdbc:postgresql://postgres:5432/osb
            - name: OSB_POSTGRES_USERNAME
              value: osb
            - name: OSB_POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: osb-db
                  key: OSB_POSTGRES_PASSWORD
            - name: OSB_N8N_BASE_URL
              value: http://host.docker.internal:5678
            - name: OSB_KEYCLOAK_URL
              value: http://host.docker.internal:8180
            - name: OSB_KEYCLOAK_REALM
              value: osb
            - name: OSB_N8N_CLIENT_TOKEN
              value: osb-n8n-client-dev-secret
          readinessProbe:
            httpGet:
              path: /q/health/ready
              port: 8080
            initialDelaySeconds: 40
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /q/health/live
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 20
---
apiVersion: v1
kind: Service
metadata:
  name: osb-api
  labels:
    app: osb-api
    osb-instance-id: ${instanceId}
spec:
  selector:
    app: osb-api
    osb-instance-id: ${instanceId}
  ports:
    - port: 8080
      targetPort: 8080
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: osb-bff
  labels:
    app: osb-bff
    osb-instance-id: ${instanceId}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: osb-bff
      osb-instance-id: ${instanceId}
  template:
    metadata:
      labels:
        app: osb-bff
        osb-instance-id: ${instanceId}
    spec:
      containers:
        - name: osb-bff
          image: ghcr.io/eumicro/osb-api/osb-bff:${parameters.image_tag}
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8081
          env:
            - name: QUARKUS_PROFILE
              value: no-auth
            - name: OSB_POSTGRES_JDBC_URL
              value: jdbc:postgresql://postgres:5432/osb
            - name: OSB_POSTGRES_USERNAME
              value: osb
            - name: OSB_POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: osb-db
                  key: OSB_POSTGRES_PASSWORD
            - name: OSB_BFF_API_URL
              value: http://osb-api:8080
            - name: OSB_BFF_N8N_BASE_URL
              value: http://host.docker.internal:5678
            - name: OSB_BFF_N8N_BRIDGE_SECRET
              value: osb-n8n-bridge-dev-secret
          readinessProbe:
            httpGet:
              path: /q/health/ready
              port: 8081
            initialDelaySeconds: 40
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /q/health/live
              port: 8081
            initialDelaySeconds: 60
            periodSeconds: 20
---
apiVersion: v1
kind: Service
metadata:
  name: osb-bff
  labels:
    app: osb-bff
    osb-instance-id: ${instanceId}
spec:
  selector:
    app: osb-bff
    osb-instance-id: ${instanceId}
  ports:
    - port: 8081
      targetPort: 8081
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: osb-ui
  labels:
    app: osb-bff
    osb-instance-id: ${instanceId}
  annotations:
    nginx.ingress.kubernetes.io/proxy-body-size: 8m
spec:
  ingressClassName: nginx
  rules:
    - host: osb-ui-${instanceId}.localhost
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: osb-bff
                port:
                  number: 8081
$tpl$,
    TRUE
),
(
    'tpl-osb-platform-bind',
    'OSB Platform Binding Secret',
    'Binding Secret with nested stack endpoints for the instance namespace.',
    'KUBERNETES_RESOURCE',
    $tpl$
apiVersion: v1
kind: Secret
metadata:
  name: osb-binding
  labels:
    app.kubernetes.io/name: osb-platform
    osb-instance-id: ${instanceId}
type: Opaque
stringData:
  apiUrl: http://osb-api:8080
  dashboardUrl: http://osb-ui-${instanceId}.localhost:8088/
  imageTag: ${parameters.image_tag}
$tpl$,
    TRUE
);

INSERT INTO workflow_definitions (
    id, name, description, kind, n8n_webhook_path, n8n_workflow_id, enabled
) VALUES
('wf-osb-provision', 'OSB Platform Provision', 'Apply nested OSB stack in namespace instanceId', 'PROVISION', '/webhook/osb-platform-provision', 'osbWfOsbProvision', TRUE),
('wf-osb-deprovision', 'OSB Platform Deprovision', 'Delete nested OSB stack', 'DEPROVISION', '/webhook/osb-platform-deprovision', 'osbWfOsbDeprovision', TRUE),
('wf-osb-update', 'OSB Platform Update', 'Re-apply nested OSB stack', 'UPDATE', '/webhook/osb-platform-update', 'osbWfOsbUpdate', TRUE),
('wf-osb-bind', 'OSB Platform Bind', 'Create binding Secret', 'BIND', '/webhook/osb-platform-bind', 'osbWfOsbBind', TRUE),
('wf-osb-unbind', 'OSB Platform Unbind', 'Delete binding Secret', 'UNBIND', '/webhook/osb-platform-unbind', 'osbWfOsbUnbind', TRUE),
('wf-osb-get-instance', 'OSB Platform Get Instance', 'GET nested OSB resources', 'GET_INSTANCE', '/webhook/osb-platform-get-instance', 'osbWfOsbGetInstance', TRUE),
('wf-osb-get-binding', 'OSB Platform Get Binding', 'GET binding Secret', 'GET_BINDING', '/webhook/osb-platform-get-binding', 'osbWfOsbGetBinding', TRUE),
('wf-osb-instance-last-op', 'OSB Platform Instance Last Op', 'GET stack as last operation', 'INSTANCE_LAST_OPERATION', '/webhook/osb-platform-instance-last-operation', 'osbWfOsbInstanceLastOp', TRUE),
('wf-osb-binding-last-op', 'OSB Platform Binding Last Op', 'GET binding Secret as last operation', 'BINDING_LAST_OPERATION', '/webhook/osb-platform-binding-last-operation', 'osbWfOsbBindingLastOp', TRUE);

INSERT INTO workflow_client_types (workflow_id, client_type)
SELECT id, 'KUBERNETES' FROM workflow_definitions WHERE id LIKE 'wf-osb-%' AND enabled;

INSERT INTO workflow_kubernetes_clients (workflow_id, kubernetes_client_id)
SELECT id, 'k8s-local-dev' FROM workflow_definitions WHERE id LIKE 'wf-osb-%' AND enabled;

INSERT INTO workflow_templates (workflow_id, template_id) VALUES
    ('wf-osb-provision', 'tpl-osb-platform'),
    ('wf-osb-deprovision', 'tpl-osb-platform'),
    ('wf-osb-update', 'tpl-osb-platform'),
    ('wf-osb-get-instance', 'tpl-osb-platform'),
    ('wf-osb-instance-last-op', 'tpl-osb-platform'),
    ('wf-osb-bind', 'tpl-osb-platform-bind'),
    ('wf-osb-unbind', 'tpl-osb-platform-bind'),
    ('wf-osb-get-binding', 'tpl-osb-platform-bind'),
    ('wf-osb-binding-last-op', 'tpl-osb-platform-bind');

INSERT INTO offering_workflows (service_id, kind, workflow_id)
SELECT 'osb-platform', kind, id
FROM workflow_definitions
WHERE id LIKE 'wf-osb-%' AND enabled;
