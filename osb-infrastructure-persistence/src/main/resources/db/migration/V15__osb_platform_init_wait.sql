-- Wait for Postgres before osb-api / osb-bff start (avoid Flyway crash loops).
UPDATE templates
SET content = $tpl$
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
      initContainers:
        - name: wait-postgres
          image: postgres:17-alpine
          command: ["sh", "-c", "until pg_isready -h postgres -U osb -d osb; do sleep 2; done"]
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
            initialDelaySeconds: 20
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /q/health/live
              port: 8080
            initialDelaySeconds: 40
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
      initContainers:
        - name: wait-postgres
          image: postgres:17-alpine
          command: ["sh", "-c", "until pg_isready -h postgres -U osb -d osb; do sleep 2; done"]
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
            initialDelaySeconds: 20
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /q/health/live
              port: 8081
            initialDelaySeconds: 40
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
$tpl$
WHERE id = 'tpl-osb-platform';
