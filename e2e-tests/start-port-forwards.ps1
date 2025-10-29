# Port-Forward Script para E2E Tests
# Este script inicia port-forwards para todos los microservicios necesarios

Write-Host "🚀 Iniciando Port-Forwards para E2E Tests..." -ForegroundColor Cyan

# Verificar que kubectl esté disponible
if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Host "❌ kubectl no encontrado. Por favor instálalo primero." -ForegroundColor Red
    exit 1
}

# Verificar que Minikube esté corriendo
$minikubeStatus = minikube status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Minikube no está corriendo. Ejecuta: minikube start" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Minikube está activo" -ForegroundColor Green

# Verificar namespace ecommerce
$namespace = kubectl get namespace ecommerce 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Namespace 'ecommerce' no encontrado" -ForegroundColor Red
    Write-Host "Crea el namespace con: kubectl create namespace ecommerce" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Namespace 'ecommerce' encontrado" -ForegroundColor Green

# Definir servicios y puertos
$services = @(
    @{ Name = "product-service"; Port = 8500 }
    @{ Name = "user-service"; Port = 8700 }
    @{ Name = "order-service"; Port = 8300 }
    @{ Name = "payment-service"; Port = 8400 }
    @{ Name = "shipping-service"; Port = 8600 }
    @{ Name = "favourite-service"; Port = 8800 }
    @{ Name = "api-gateway"; Port = 8080 }
)

# Array para almacenar procesos
$global:portForwardJobs = @()

# Función para iniciar port-forward en background
function Start-PortForward {
    param(
        [string]$ServiceName,
        [int]$Port
    )
    
    Write-Host "🔌 Port-forwarding: $ServiceName → localhost:$Port" -ForegroundColor Yellow
    
    # Iniciar kubectl port-forward en background job
    $job = Start-Job -ScriptBlock {
        param($svc, $port, $ns)
    kubectl port-forward -n $ns service/$svc ${port}:${port}
    } -ArgumentList $ServiceName, $Port, "ecommerce"
    
    $global:portForwardJobs += $job
    
    # Esperar un momento para que se establezca la conexión
    Start-Sleep -Seconds 2
    
    # Verificar que el puerto esté escuchando
    $listening = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($listening) {
        Write-Host "✅ $ServiceName escuchando en puerto $Port" -ForegroundColor Green
        return $true
    } else {
        Write-Host "⚠️  $ServiceName iniciado pero aún no escucha en puerto $Port" -ForegroundColor Yellow
        return $false
    }
}

# Verificar que los pods estén corriendo
Write-Host "`n📦 Verificando pods en namespace ecommerce..." -ForegroundColor Cyan
$pods = kubectl get pods -n ecommerce --no-headers 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Error al obtener pods: $pods" -ForegroundColor Red
    exit 1
}

$runningPods = ($pods | Select-String -Pattern "Running").Count
Write-Host "✅ $runningPods pods en estado Running" -ForegroundColor Green

if ($runningPods -eq 0) {
    Write-Host "❌ No hay pods corriendo. Despliega los servicios primero." -ForegroundColor Red
    exit 1
}

# Iniciar port-forwards para cada servicio
Write-Host "`n🔌 Iniciando port-forwards..." -ForegroundColor Cyan
$successCount = 0

foreach ($service in $services) {
    $success = Start-PortForward -ServiceName $service.Name -Port $service.Port
    if ($success) {
        $successCount++
    }
}

Write-Host "`n✅ $successCount/$($services.Count) port-forwards activos" -ForegroundColor Green

# Mostrar URLs disponibles
Write-Host "`n🌐 URLs Disponibles:" -ForegroundColor Cyan
Write-Host "  - API Gateway:       http://localhost:8080" -ForegroundColor White
Write-Host "  - Product Service:   http://localhost:8500/product-service/api/products" -ForegroundColor White
Write-Host "  - User Service:      http://localhost:8700/user-service/api/users" -ForegroundColor White
Write-Host "  - Order Service:     http://localhost:8300/order-service/api/orders" -ForegroundColor White
Write-Host "  - Payment Service:   http://localhost:8400/payment-service/api/payments" -ForegroundColor White
Write-Host "  - Shipping Service:  http://localhost:8600/shipping-service/api/order-items" -ForegroundColor White
Write-Host "  - Favourite Service: http://localhost:8800/favourite-service/api/favourites" -ForegroundColor White

# Instrucciones para ejecutar tests
Write-Host "`n🧪 Para ejecutar los E2E tests:" -ForegroundColor Cyan
Write-Host "  cd e2e-tests" -ForegroundColor Yellow
Write-Host "  mvn clean test" -ForegroundColor Yellow

# Mantener el script corriendo
Write-Host "`n⏸️  Port-forwards activos. Presiona Ctrl+C para detener..." -ForegroundColor Green
Write-Host "   Los jobs de port-forward seguirán corriendo en background.`n" -ForegroundColor Gray

# Registrar función de cleanup
$cleanup = {
    Write-Host "`n🛑 Deteniendo port-forwards..." -ForegroundColor Yellow
    foreach ($job in $global:portForwardJobs) {
        Stop-Job -Job $job -ErrorAction SilentlyContinue
        Remove-Job -Job $job -Force -ErrorAction SilentlyContinue
    }
    Write-Host "✅ Port-forwards detenidos" -ForegroundColor Green
}

# Registrar evento de Ctrl+C
[Console]::TreatControlCAsInput = $false
Register-EngineEvent -SourceIdentifier PowerShell.Exiting -Action $cleanup

# Esperar indefinidamente
try {
    while ($true) {
        Start-Sleep -Seconds 5
        
        # Verificar que los jobs sigan vivos
        $aliveJobs = ($global:portForwardJobs | Where-Object { $_.State -eq 'Running' }).Count
        
        if ($aliveJobs -lt $services.Count) {
            Write-Host "⚠️  Algunos port-forwards se detuvieron ($aliveJobs/$($services.Count) activos)" -ForegroundColor Yellow
        }
    }
}
finally {
    & $cleanup
}
