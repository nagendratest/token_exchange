# Kubernetes Manifests

Deploy with:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

Namespace:

`apigw-token-service`

The service is internal-only (`ClusterIP`) and reachable by other workloads in the same namespace at:

`http://token-generator-service:8081`

Cross-namespace call format:

`http://token-generator-service.<namespace>.svc.cluster.local:8081`
