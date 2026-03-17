# Helm Chart: token-generator-service

## Install

```bash
kubectl apply -f ./helm/token-generator-service/namespace.yaml

helm upgrade --install token-generator-service ./helm/token-generator-service \
  --namespace apigw-token-service
```

## Override image

```bash
kubectl apply -f ./helm/token-generator-service/namespace.yaml

helm upgrade --install token-generator-service ./helm/token-generator-service \
  --namespace apigw-token-service \
  --set image.repository=<your-registry>/token-generator-service \
  --set image.tag=<your-tag>
```

## Internal service URL

Same namespace:

`http://<helm-service-name>:8081`

Cross-namespace:

`http://<helm-service-name>.<namespace>.svc.cluster.local:8081`

If you install with release name `token-generator-service`, the service name is `token-generator-service`.

## Connect from another microservice

If another microservice is running in the same Kubernetes cluster, call the Kubernetes Service DNS name instead of `localhost`.

### Get the correct namespace

If you used the install command in this README, the namespace is `apigw-token-service`.

To verify the actual namespace used by the Helm release:

```bash
helm list --all-namespaces
```

Check the `NAMESPACE` column for your `token-generator-service` release.

### Get the correct service name

List services in that namespace:

```bash
kubectl get svc -n apigw-token-service
```

Use the value from the `NAME` column.

Common result with the default release name:

```text
token-generator-service
```

If you installed with a different Helm release name, the service name can be different, so verify it with `kubectl get svc` instead of assuming it.

### Build the correct URL

If the calling microservice is in the same namespace as `token-generator-service`:

```text
http://<service-name>:8081
```

Example:

```text
http://token-generator-service:8081
```

If the calling microservice is in a different namespace:

```text
http://<service-name>.<namespace>.svc.cluster.local:8081
```

Example:

```text
http://token-generator-service.apigw-token-service.svc.cluster.local:8081
```

### Example endpoint URLs

```text
POST http://token-generator-service:8081/eorchestrator/tokens
POST http://token-generator-service:8081/enm/login
```

### Example Spring Boot configuration

```yaml
token-generator:
  base-url: http://token-generator-service:8081
```

If the caller is in another namespace, use the full cluster URL instead.

## Test with curl

If you installed with the default release name, forward the service with:

```bash
kubectl port-forward -n apigw-token-service svc/token-generator-service 8081:8081
```

If the service name is different in your cluster, replace `token-generator-service` with the value returned by `kubectl get svc -n apigw-token-service`.

In a second terminal, call the ENM login endpoint. A username containing `testuser` returns a fake token from the service, so you can verify the deployment without depending on the upstream ENM system.

```bash
curl -X POST http://localhost:8081/enm/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser1","password":"secret","tenantId":"ECM"}'
```
