# Centralized Logging (ELK)

The manifests in this folder deploy the required Elasticsearch + Logstash + Kibana stack and Filebeat agents that collect container logs from the `ecommerce` namespace.

## Components

- `elasticsearch-statefulset.yaml`: single-node Elasticsearch with persistent storage.
- `logstash-deployment.yaml`: Logstash deployment with a pipeline that accepts Beats input and forwards documents to Elasticsearch.
- `filebeat-daemonset.yaml`: Filebeat runs on every node, tails container logs from the `ecommerce` namespace and ships them to Logstash.
- `kibana-deployment.yaml`: Kibana UI connected to Elasticsearch for log exploration.

## Deploy

```bash
kubectl apply -f k8s/observability/namespace.yaml # once
kubectl apply -f k8s/logging/elasticsearch-statefulset.yaml
kubectl apply -f k8s/logging/logstash-deployment.yaml
kubectl apply -f k8s/logging/filebeat-daemonset.yaml
kubectl apply -f k8s/logging/kibana-deployment.yaml
```

Expose Kibana in your environment via:

```bash
kubectl -n observability port-forward svc/kibana 5601:5601
```

The Filebeat configuration limits collection to pods running inside the `ecommerce` namespace to avoid interfering with other workloads.
