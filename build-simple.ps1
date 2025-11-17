# Build Docker Images - Copia JARs fuera de target/ para evitar .dockerignore

$ACR_SERVER = "ecommercerojas435acr.azurecr.io"

$SERVICES = @(
    "order-service",
    "payment-service", 
    "shipping-service",
    "product-service",
    "user-service",
    "favourite-service",
    "proxy-client",
    "api-gateway",
    "service-discovery",
    "cloud-config"
)

Write-Host "=== CONSTRUYENDO IMÁGENES (EVITANDO .dockerignore) ===" -ForegroundColor Cyan
Write-Host ""

foreach ($SERVICE in $SERVICES) {
    Write-Host "[$SERVICE] Verificando JAR compilado..." -ForegroundColor Yellow
    
    # Buscar el JAR ejecutable (Spring Boot repackaged)
    $JAR_FILE = Get-ChildItem "$SERVICE\target" -Filter "*.jar" | Where-Object { $_.Name -notlike "*original*" } | Select-Object -First 1
    
    if (-not $JAR_FILE) {
        Write-Host "[$SERVICE] ERROR: JAR no encontrado en target/" -ForegroundColor Red
        continue
    }
    
    Write-Host "[$SERVICE] JAR encontrado: $($JAR_FILE.Name)" -ForegroundColor Green
    
    # COPIAR JAR fuera de target/ (para evitar .dockerignore)
    Copy-Item "$SERVICE\target\$($JAR_FILE.Name)" "$SERVICE\app.jar" -Force
    Write-Host "[$SERVICE] JAR copiado a $SERVICE\app.jar" -ForegroundColor Green
    
    # Crear Dockerfile que copia desde root del servicio (no desde target/)
    $DOCKERFILE_CONTENT = @"
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
"@

    $DOCKERFILE_CONTENT | Out-File -FilePath "$SERVICE\Dockerfile.final" -Encoding utf8
    
    Write-Host "[$SERVICE] Construyendo imagen..." -ForegroundColor Cyan
    docker build -t "${ACR_SERVER}/${SERVICE}:latest" -f "$SERVICE\Dockerfile.final" "$SERVICE"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[$SERVICE] ERROR: Falló construcción" -ForegroundColor Red
        Remove-Item "$SERVICE\app.jar" -ErrorAction SilentlyContinue
        continue
    }
    
    Write-Host "[$SERVICE] Subiendo a ACR..." -ForegroundColor Cyan
    docker push "${ACR_SERVER}/${SERVICE}:latest"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[$SERVICE] Completado exitosamente" -ForegroundColor Green
    } else {
        Write-Host "[$SERVICE] ERROR: Falló subida" -ForegroundColor Red
    }
    
    # Limpiar JAR temporal
    Remove-Item "$SERVICE\app.jar" -ErrorAction SilentlyContinue
    
    Write-Host ""
}

Write-Host "=== RESUMEN ===" -ForegroundColor Cyan
az acr repository list --name ecommercerojas435acr --output table
