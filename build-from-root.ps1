# Estrategia final: Build desde root con toda la estructura

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

Write-Host "=== CONSTRUYENDO DESDE RAÍZ DEL PROYECTO ===" -ForegroundColor Cyan

foreach ($SERVICE in $SERVICES) {
    Write-Host "[$SERVICE] Construyendo..." -ForegroundColor Yellow
    
    # Dockerfile que construye desde raíz del proyecto
    $DOCKERFILE = @"
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY $SERVICE/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
"@

    $DOCKERFILE | Out-File -FilePath "Dockerfile.$SERVICE" -Encoding utf8
    
    # Build desde RAÍZ (no desde subdirectorio)
    docker build -t "${ACR_SERVER}/${SERVICE}:latest" -f "Dockerfile.$SERVICE" .
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[$SERVICE] Imagen OK, subiendo..." -ForegroundColor Cyan
        docker push "${ACR_SERVER}/${SERVICE}:latest"
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[$SERVICE] COMPLETADO" -ForegroundColor Green
        }
    } else {
        Write-Host "[$SERVICE] ERROR" -ForegroundColor Red
    }
    
    Remove-Item "Dockerfile.$SERVICE" -ErrorAction SilentlyContinue
    Write-Host ""
}

Write-Host "=== VERIFICANDO ACR ===" -ForegroundColor Cyan
az acr repository list --name ecommercerojas435acr --output table
