# Observability Stack

This folder contains Kubernetes manifests that deploy the monitoring stack required by the project.

## Components

| File | Description |
| --- | --- |
| `namespace.yaml` | Dedicated `observability` namespace to isolate monitoring workloads. |
| `prometheus-configmap.yaml` | Prometheus scrape configuration, alerting rules for technical and business KPIs. |
| `prometheus-deployment.yaml` | RBAC, PVC, Deployment and Service for Prometheus. |
| `grafana-configmap.yaml` | Provisioned Prometheus datasource plus default dashboards. |
| `grafana-deployment.yaml` | Grafana Deployment, persistent storage and admin secret. |
| `alertmanager-config.yaml` | Alertmanager configuration + Deployment and Service. |
| `zipkin.yaml` | Zipkin deployment and service that receives spans from all microservices. |

## Deploy

```bash
kubectl apply -f k8s/observability/namespace.yaml
kubectl apply -f k8s/observability/prometheus-configmap.yaml
kubectl apply -f k8s/observability/prometheus-deployment.yaml
kubectl apply -f k8s/observability/grafana-configmap.yaml
kubectl apply -f k8s/observability/grafana-deployment.yaml
kubectl apply -f k8s/observability/alertmanager-config.yaml
kubectl apply -f k8s/observability/zipkin.yaml
```

Grafana becomes accessible via port-forward:

```bash
kubectl -n observability port-forward svc/grafana 3000:3000
```

Credentials default to `admin / admin1234` and should be rotated in a secret for production.
