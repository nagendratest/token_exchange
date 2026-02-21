# Token Exchange Service

## Run

```
docker compose up --build
```

The Spring Boot API will be available on `http://localhost:8081`.

## Example Request

### EOrchestrator

```
curl -X POST http://localhost:8081/eorchestrator/tokens \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"pass1","tenantId":"ECM"}'
```

### ENM

```
curl -X POST http://localhost:8081/enm/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"secret"}'
```
