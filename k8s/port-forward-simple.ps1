# Port-Forward Simple para Kubernetes
# Abre port-forwards para todos los servicios

Write-Host ""
Write-Host "Iniciando Port-Forwards para Kubernetes..." -ForegroundColor Cyan
Write-Host ""
Write-Host "IMPORTANTE: Deja esta ventana ABIERTA mientras uses Postman" -ForegroundColor Yellow
Write-Host "Presiona Ctrl+C para detener" -ForegroundColor Yellow
Write-Host ""

# Verificar namespace
$check = kubectl get namespace ecommerce 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: El namespace 'ecommerce' no existe" -ForegroundColor Red
    Write-Host "Ejecuta: kubectl apply -f k8s/all-in-one.yaml" -ForegroundColor Yellow
    exit 1
}

# Limpiar jobs anteriores
Get-Job | Remove-Job -Force 2>$null

Write-Host "Configurando Port-Forwards..." -ForegroundColor Cyan
Write-Host ""

# API Gateway
Write-Host "[OK] API Gateway       -> http://localhost:8080" -ForegroundColor Green
Start-Job -ScriptBlock { kubectl port-forward -n ecommerce service/api-gateway 8080:8080 } | Out-Null
Start-Sleep -Milliseconds 300

# Product Service
Write-Host "[OK] Product Service   -> http://localhost:8500" -ForegroundColor Green
Start-Job -ScriptBlock { kubectl port-forward -n ecommerce service/product-service 8500:8500 } | Out-Null
Start-Sleep -Milliseconds 300

# User Service
Write-Host "[OK] User Service      -> http://localhost:8400" -ForegroundColor Green
Start-Job -ScriptBlock { kubectl port-forward -n ecommerce service/user-service 8400:8400 } | Out-Null
Start-Sleep -Milliseconds 300

# Favourite Service
Write-Host "[OK] Favourite Service -> http://localhost:8800" -ForegroundColor Green
Start-Job -ScriptBlock { kubectl port-forward -n ecommerce service/favourite-service 8800:8800 } | Out-Null
Start-Sleep -Milliseconds 300

# Shipping Service
Write-Host "[OK] Shipping Service  -> http://localhost:8700" -ForegroundColor Green
Start-Job -ScriptBlock { kubectl port-forward -n ecommerce service/shipping-service 8700:8700 } | Out-Null
Start-Sleep -Milliseconds 300

# Payment Service
Write-Host "[OK] Payment Service   -> http://localhost:8600" -ForegroundColor Green
Start-Job -ScriptBlock { kubectl port-forward -n ecommerce service/payment-service 8600:8600 } | Out-Null

# Order Service
Write-Host "[OK] Order Service     -> http://localhost:8300" -ForegroundColor Green
Start-Job -ScriptBlock { kubectl port-forward -n ecommerce service/order-service 8300:8300 } | Out-Null

Start-Sleep -Seconds 2

Write-Host ""
Write-Host "=========================================================" -ForegroundColor Green
Write-Host " Port-Forwards ACTIVOS - Usa Postman con localhost" -ForegroundColor Green
Write-Host "=========================================================" -ForegroundColor Green
Write-Host ""
Write-Host "URLs para Postman:" -ForegroundColor Cyan
Write-Host "  http://localhost:8080/product-service/api/products" -ForegroundColor White
Write-Host "  http://localhost:8500/product-service/api/products" -ForegroundColor White
Write-Host "  http://localhost:8400/user-service/api/users" -ForegroundColor White
Write-Host "  http://localhost:8800/favourite-service/api/favourites" -ForegroundColor White
Write-Host "  http://localhost:8300/order-service/api/orders" -ForegroundColor White
Write-Host ""
Write-Host "Importa la coleccion de Postman:" -ForegroundColor Yellow
Write-Host "  postman/Ecommerce-Kubernetes.postman_collection.json" -ForegroundColor White
Write-Host ""
Write-Host "=========================================================" -ForegroundColor Green
Write-Host ""
Write-Host "MANTEN esta ventana abierta (Ctrl+C para detener)" -ForegroundColor Yellow
Write-Host ""

try {
    # Mantener corriendo
    while ($true) {
        Start-Sleep -Seconds 10
    }
}
finally {
    Write-Host ""
    Write-Host "Deteniendo Port-Forwards..." -ForegroundColor Yellow
    Get-Job | Stop-Job
    Get-Job | Remove-Job -Force
    Write-Host "Detenidos OK" -ForegroundColor Green
}
