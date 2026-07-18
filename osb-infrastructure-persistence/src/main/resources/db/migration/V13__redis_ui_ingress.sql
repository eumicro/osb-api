-- Redis Commander exposed via Kind ingress-nginx: http://redis-ui-<instanceId>.localhost/

UPDATE templates
SET description = 'Redis + Redis Commander UI behind Ingress redis-ui-<instanceId>.localhost',
    content = $tpl$
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  labels:
    app: redis
    osb-instance-id: ${instanceId}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
      osb-instance-id: ${instanceId}
  template:
    metadata:
      labels:
        app: redis
        osb-instance-id: ${instanceId}
    spec:
      containers:
        - name: redis
          image: redis:7-alpine
          ports:
            - containerPort: 6379
          resources:
            limits:
              memory: ${parameters.memory_mb}Mi
---
apiVersion: v1
kind: Service
metadata:
  name: redis
  labels:
    app: redis
    osb-instance-id: ${instanceId}
spec:
  selector:
    app: redis
    osb-instance-id: ${instanceId}
  ports:
    - port: 6379
      targetPort: 6379
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis-ui
  labels:
    app: redis-ui
    osb-instance-id: ${instanceId}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis-ui
      osb-instance-id: ${instanceId}
  template:
    metadata:
      labels:
        app: redis-ui
        osb-instance-id: ${instanceId}
    spec:
      containers:
        - name: redis-commander
          image: rediscommander/redis-commander:latest
          env:
            - name: REDIS_HOSTS
              value: local:redis:6379
          ports:
            - containerPort: 8081
---
apiVersion: v1
kind: Service
metadata:
  name: redis-ui
  labels:
    app: redis-ui
    osb-instance-id: ${instanceId}
spec:
  selector:
    app: redis-ui
    osb-instance-id: ${instanceId}
  ports:
    - port: 8081
      targetPort: 8081
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: redis-ui
  labels:
    app: redis-ui
    osb-instance-id: ${instanceId}
  annotations:
    nginx.ingress.kubernetes.io/proxy-body-size: 8m
spec:
  ingressClassName: nginx
  rules:
    - host: redis-ui-${instanceId}.localhost
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: redis-ui
                port:
                  number: 8081
$tpl$
WHERE id = 'tpl-redis-cache';
