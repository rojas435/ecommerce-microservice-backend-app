# Script para detener Jenkins y SonarQube
# Ejecutar con: .\stop-jenkins.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   DETENIENDO SERVICIOS JENKINS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$action = Read-Host "Que deseas hacer?
1) Detener servicios (mantener datos)
2) Detener y eliminar contenedores (mantener datos)
3) Eliminar TODO (incluyendo datos)
Selecciona (1/2/3)"

switch ($action) {
    "1" {
        Write-Host ""
        Write-Host "Deteniendo servicios..." -ForegroundColor Yellow
        docker-compose stop
        Write-Host "Servicios detenidos (datos preservados)" -ForegroundColor Green
    }
    "2" {
        Write-Host ""
        Write-Host "Deteniendo y eliminando contenedores..." -ForegroundColor Yellow
        docker-compose down
        Write-Host "Contenedores eliminados (datos preservados)" -ForegroundColor Green
    }
    "3" {
        Write-Host ""
        Write-Host "ADVERTENCIA: Esto eliminara TODOS los datos de Jenkins y SonarQube" -ForegroundColor Red
        $confirm = Read-Host "Estas seguro? Escribe 'SI' para confirmar"
        
        if ($confirm -eq "SI") {
            Write-Host "Eliminando todo..." -ForegroundColor Yellow
            docker-compose down -v
            Write-Host "Todo eliminado (incluyendo datos)" -ForegroundColor Green
        } else {
            Write-Host "Operacion cancelada" -ForegroundColor Yellow
        }
    }
    default {
        Write-Host "Opcion invalida" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "Para volver a iniciar:" -ForegroundColor Cyan
Write-Host "  .\start-jenkins.ps1" -ForegroundColor White
Write-Host ""
