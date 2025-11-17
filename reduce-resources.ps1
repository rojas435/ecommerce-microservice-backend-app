# Script para reducir los requisitos de recursos de los pods
# Esto permite ejecutar más pods en el cluster pequeño de 2 vCPUs

$NAMESPACE = "ecommerce"

Write-Host "Reduciendo requisitos de recursos de los deployments..." -ForegroundColor Cyan

$DEPLOYMENTS = @(
    "api-gateway",
    "order-service",
    "payment-service",
    "product-service",
    "shipping-service",
    "user-service",
    "favourite-service"
)

foreach ($DEPLOY in $DEPLOYMENTS) {
    Write-Host "[$DEPLOY] Ajustando recursos..." -ForegroundColor Yellow
    
    kubectl patch deployment $DEPLOY -n $NAMESPACE --type='json' -p='[
        {
            "op": "replace",
            "path": "/spec/template/spec/containers/0/resources",
            "value": {
                "requests": {
                    "cpu": "100m",
                    "memory": "256Mi"
                },
                "limits": {
                    "cpu": "200m",
                    "memory": "512Mi"
                }
            }
        }
    ]'
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[$DEPLOY] Recursos ajustados" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Esperando 20 segundos para que los pods se actualicen..." -ForegroundColor Yellow
Start-Sleep -Seconds 20

Write-Host ""
Write-Host "Estado de pods:" -ForegroundColor Cyan
kubectl get pods -n $NAMESPACE
