# Script para reiniciar Minikube con 18GB RAM
# IMPORTANTE: Antes de ejecutar este script, asegúrate de haber aumentado 
# los recursos en Docker Desktop a 18GB RAM y 6 CPUs

Write-Host "=== Reiniciando Minikube con 18GB RAM ===" -ForegroundColor Cyan
Write-Host ""

# Verificar que Docker Desktop tenga suficiente memoria
Write-Host "Verificando recursos de Docker Desktop..." -ForegroundColor Yellow
docker info | Select-String "Total Memory"

Write-Host ""
Write-Host "Si ves menos de 18GB arriba, DETENTE y aumenta los recursos en Docker Desktop primero!" -ForegroundColor Red
Write-Host "Presiona ENTER para continuar o CTRL+C para cancelar..."
Read-Host

Write-Host ""
Write-Host "Iniciando Minikube con configuración optimizada..." -ForegroundColor Green
minikube start --cpus=6 --memory=18432 --disk-size=50g

Write-Host ""
Write-Host "Esperando a que el cluster esté listo..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host ""
Write-Host "Estado del cluster:" -ForegroundColor Cyan
minikube status

Write-Host ""
Write-Host "Recursos del nodo:" -ForegroundColor Cyan
kubectl top node

Write-Host ""
Write-Host "=== Minikube reiniciado exitosamente ===" -ForegroundColor Green
Write-Host ""
Write-Host "SIGUIENTE PASO: Redeployar Jenkins y SonarQube" -ForegroundColor Cyan
Write-Host "Ejecuta: .\redeploy-ci-cd.ps1" -ForegroundColor White
