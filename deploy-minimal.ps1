# Configuración simplificada para cluster de 2 vCPUs
# Solo despliega los servicios más críticos con recursos mínimos

$ACR_SERVER = "ecommercerojas435acr.azurecr.io"
$NAMESPACE = "ecommerce"

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "Desplegando servicios críticos en cluster pequeño" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

# Servicios críticos a desplegar
$CRITICAL_SERVICES = @(
    "api-gateway",
    "order-service",
    "product-service",
    "user-service"
)

foreach ($SERVICE in $CRITICAL_SERVICES) {
    Write-Host "[$SERVICE] Desplegando..." -ForegroundColor Cyan
    
    kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: $SERVICE
  namespace: $NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: $SERVICE
  template:
    metadata:
      labels:
        app: $SERVICE
    spec:
      containers:
      - name: $SERVICE
        image: ${ACR_SERVER}/${SERVICE}:latest
        ports:
        - containerPort: 8080
        resources:
          requests:
            cpu: "100m"
            memory: "256Mi"
          limits:
            cpu: "250m"
            memory: "512Mi"
---
apiVersion: v1
kind: Service
metadata:
  name: $SERVICE
  namespace: $NAMESPACE
spec:
  type: ClusterIP
  ports:
  - port: 8080
    targetPort: 8080
  selector:
    app: $SERVICE
EOF

    if ($LASTEXITCODE -eq 0) {
        Write-Host "[$SERVICE] Desplegado" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Esperando 30 segundos..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

kubectl get pods -n $NAMESPACE
Write-Host ""
Write-Host "Para ver logs: kubectl logs -f POD_NAME -n $NAMESPACE" -ForegroundColor Cyan
