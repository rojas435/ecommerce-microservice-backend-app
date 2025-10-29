# Script de Inicio y Configuración de Jenkins
# Ejecutar con: .\start-jenkins.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   INICIANDO JENKINS + SONARQUBE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar Docker Desktop
Write-Host "[1/6] Verificando Docker Desktop..." -ForegroundColor Yellow
$dockerRunning = docker ps 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Docker Desktop no esta corriendo" -ForegroundColor Red
    Write-Host "Por favor inicia Docker Desktop y vuelve a ejecutar este script" -ForegroundColor Red
    exit 1
}
Write-Host "     Docker Desktop: OK" -ForegroundColor Green
Write-Host ""

# Verificar Minikube
Write-Host "[2/6] Verificando Minikube..." -ForegroundColor Yellow
$minikubeStatus = minikube status 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ADVERTENCIA: Minikube no esta corriendo" -ForegroundColor Yellow
    Write-Host "Jenkins necesita acceso a Kubernetes para los deploys" -ForegroundColor Yellow
    $continue = Read-Host "Deseas iniciar Minikube ahora? (s/n)"
    if ($continue -eq "s") {
        Write-Host "Iniciando Minikube..." -ForegroundColor Yellow
        minikube start
    }
}
Write-Host "     Minikube: OK" -ForegroundColor Green
Write-Host ""

# Verificar puertos disponibles
Write-Host "[3/6] Verificando puertos disponibles..." -ForegroundColor Yellow
$port8081 = netstat -ano | Select-String "8081.*LISTENING"
$port9000 = netstat -ano | Select-String "9000.*LISTENING"

if ($port8081) {
    Write-Host "ADVERTENCIA: Puerto 8081 ya esta en uso" -ForegroundColor Red
    Write-Host "Jenkins necesita el puerto 8081 libre" -ForegroundColor Red
    $continue = Read-Host "Deseas continuar de todas formas? (s/n)"
    if ($continue -ne "s") {
        exit 1
    }
}

if ($port9000) {
    Write-Host "ADVERTENCIA: Puerto 9000 ya esta en uso" -ForegroundColor Red
    Write-Host "SonarQube necesita el puerto 9000 libre" -ForegroundColor Red
    $continue = Read-Host "Deseas continuar de todas formas? (s/n)"
    if ($continue -ne "s") {
        exit 1
    }
}
Write-Host "     Puertos 8081 y 9000: OK" -ForegroundColor Green
Write-Host ""

# Detener servicios previos si existen
Write-Host "[4/6] Limpiando servicios previos..." -ForegroundColor Yellow
docker-compose down 2>$null
Write-Host "     Limpieza completada" -ForegroundColor Green
Write-Host ""

# Iniciar servicios
Write-Host "[5/6] Iniciando servicios (esto puede tardar 2-3 minutos)..." -ForegroundColor Yellow
docker-compose up -d

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: No se pudieron iniciar los servicios" -ForegroundColor Red
    Write-Host "Revisa los logs con: docker-compose logs" -ForegroundColor Red
    exit 1
}
Write-Host "     Servicios iniciados" -ForegroundColor Green
Write-Host ""

# Esperar a que Jenkins esté listo
Write-Host "[6/6] Esperando a que Jenkins este listo..." -ForegroundColor Yellow
$maxAttempts = 30
$attempt = 0
$jenkinsReady = $false

while ($attempt -lt $maxAttempts) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8081" -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200 -or $response.StatusCode -eq 403) {
            $jenkinsReady = $true
            break
        }
    } catch {
        # Jenkins aún no está listo
    }
    
    $attempt++
    Write-Host "     Intento $attempt/$maxAttempts..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
}

if ($jenkinsReady) {
    Write-Host "     Jenkins: LISTO" -ForegroundColor Green
} else {
    Write-Host "ADVERTENCIA: Jenkins tarda en iniciarse" -ForegroundColor Yellow
    Write-Host "Puedes revisar el progreso con: docker-compose logs -f jenkins" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   SERVICIOS INICIADOS EXITOSAMENTE" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Jenkins:    http://localhost:8081" -ForegroundColor White
Write-Host "            Usuario: admin" -ForegroundColor Gray
Write-Host "            Password: admin123" -ForegroundColor Gray
Write-Host ""
Write-Host "SonarQube:  http://localhost:9000" -ForegroundColor White
Write-Host "            Usuario: admin" -ForegroundColor Gray
Write-Host "            Password: admin (cambiar al primer login)" -ForegroundColor Gray
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Mostrar logs
$showLogs = Read-Host "Deseas ver los logs en tiempo real? (s/n)"
if ($showLogs -eq "s") {
    Write-Host ""
    Write-Host "Mostrando logs (Ctrl+C para salir)..." -ForegroundColor Yellow
    Write-Host ""
    docker-compose logs -f
}

Write-Host ""
Write-Host "Comandos utiles:" -ForegroundColor Cyan
Write-Host "  Ver logs:        docker-compose logs -f" -ForegroundColor White
Write-Host "  Ver servicios:   docker-compose ps" -ForegroundColor White
Write-Host "  Detener:         docker-compose stop" -ForegroundColor White
Write-Host "  Reiniciar:       docker-compose restart" -ForegroundColor White
Write-Host ""
