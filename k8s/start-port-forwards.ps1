# Script para abrir Port-Forwards de todos los servicios en Kubernetes
# Esto permite acceder a los servicios como si estuvieran en Docker Compose

Write-Host "🚀 Iniciando Port-Forwards para todos los servicios..." -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  IMPORTANTE: Deja esta ventana ABIERTA mientras uses Postman" -ForegroundColor Yellow
Write-Host "    Para detener, presiona Ctrl+C" -ForegroundColor Yellow
Write-Host ""

# Función para iniciar port-forward en background
function Start-PortForward {
    param(
        [string]$ServiceName,
        [int]$Port
    )
    
    Write-Host "✅ Port-Forward activo: $ServiceName -> http://localhost:$Port" -ForegroundColor Green
    
    Start-Job -ScriptBlock {
        param($svc, $p, $ns)
        $portMapping = "${p}:${p}"
        kubectl port-forward -n $ns service/$svc $portMapping
    } -ArgumentList $ServiceName, $Port, "ecommerce" | Out-Null
}

# Verificar que el namespace existe
$namespaceCheck = kubectl get namespace ecommerce 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ ERROR: El namespace 'ecommerce' no existe." -ForegroundColor Red
    Write-Host "   Primero despliega la aplicación con:" -ForegroundColor Yellow
    Write-Host "   kubectl apply -f k8s/all-in-one.yaml" -ForegroundColor White
    exit 1
}

# Verificar que los pods están corriendo
$podsReady = (kubectl get pods -n ecommerce --no-headers 2>&1 | Where-Object { $_ -match "1/1.*Running" }).Count
$totalPods = (kubectl get pods -n ecommerce --no-headers 2>&1 | Measure-Object).Count

Write-Host "📊 Estado de los pods: $podsReady/$totalPods Ready" -ForegroundColor Cyan
if ($podsReady -lt $totalPods) {
    Write-Host "⚠️  Algunos pods no están listos todavía. Port-forwards pueden fallar." -ForegroundColor Yellow
    Write-Host "   Espera unos segundos y vuelve a ejecutar este script." -ForegroundColor Yellow
    Write-Host ""
}

# Iniciar port-forwards
Write-Host ""
Write-Host "🔌 Configurando Port-Forwards..." -ForegroundColor Cyan
Write-Host ""

Start-PortForward "api-gateway" 8080
Start-Sleep -Milliseconds 500

Start-PortForward "product-service" 8500
Start-Sleep -Milliseconds 500

Start-PortForward "user-service" 8400
Start-Sleep -Milliseconds 500

Start-PortForward "favourite-service" 8800
Start-Sleep -Milliseconds 500

Start-PortForward "shipping-service" 8700
Start-Sleep -Milliseconds 500

Start-PortForward "payment-service" 8600
Start-Sleep -Milliseconds 500

# Order Service
Start-PortForward "order-service" 8300

Start-Sleep -Seconds 2

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "✅ Todos los Port-Forwards están activos!" -ForegroundColor Green
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host ""
Write-Host "📮 Ahora puedes usar Postman con estas URLs:" -ForegroundColor Cyan
Write-Host ""
Write-Host "   API Gateway:         http://localhost:8080" -ForegroundColor White
Write-Host "   Product Service:     http://localhost:8500" -ForegroundColor White
Write-Host "   User Service:        http://localhost:8400" -ForegroundColor White
Write-Host "   Favourite Service:   http://localhost:8800" -ForegroundColor White
Write-Host "   Shipping Service:    http://localhost:8700" -ForegroundColor White
Write-Host "   Payment Service:     http://localhost:8600" -ForegroundColor White
Write-Host "   Order Service:       http://localhost:8300" -ForegroundColor White
Write-Host ""
Write-Host "🧪 Ejemplos de endpoints:" -ForegroundColor Cyan
Write-Host "   GET http://localhost:8500/product-service/api/products" -ForegroundColor Yellow
Write-Host "   GET http://localhost:8700/user-service/api/users" -ForegroundColor Yellow
Write-Host "   GET http://localhost:8080/product-service/api/products (vía Gateway)" -ForegroundColor Yellow
Write-Host "   GET http://localhost:8080/app/api/products (Proxy)" -ForegroundColor Yellow
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host ""
Write-Host "⚠️  Mantén esta ventana ABIERTA" -ForegroundColor Yellow
Write-Host "   Para detener los Port-Forwards, presiona Ctrl+C" -ForegroundColor Yellow
Write-Host ""

# Mantener el script corriendo
Write-Host "🔄 Monitoreando Port-Forwards (Ctrl+C para salir)..." -ForegroundColor Cyan
Write-Host ""

try {
    while ($true) {
        Start-Sleep -Seconds 5
        
        # Verificar que los jobs están corriendo
        $runningJobs = (Get-Job | Where-Object { $_.State -eq "Running" }).Count
        
        if ($runningJobs -lt 6) {
            Write-Host "⚠️  Algunos port-forwards se detuvieron. Relanzando..." -ForegroundColor Yellow
            
            # Limpiar jobs
            Get-Job | Remove-Job -Force
            
            # Reiniciar
            & $PSCommandPath
            exit
        }
    }
}
finally {
    Write-Host ""
    Write-Host "🛑 Deteniendo Port-Forwards..." -ForegroundColor Yellow
    Get-Job | Stop-Job
    Get-Job | Remove-Job -Force
    Write-Host "✅ Port-Forwards detenidos." -ForegroundColor Green
}
