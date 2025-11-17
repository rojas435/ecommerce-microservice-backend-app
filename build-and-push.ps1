# Build and Push Images with Pre-Compiled JARs
# Este script usa los JARs compilados localmente con Maven (mvn clean package -DskipTests)

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

$ACR_SERVER = "ecommercerojas435acr.azurecr.io"

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "CONSTRUYENDO IMÁGENES CON JARs PRE-COMPILADOS" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
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
    
    # Crear Dockerfile simple que copia el JAR ya compilado
    $DOCKERFILE_CONTENT = @"
# Imagen base ligera con Java
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar el JAR pre-compilado
COPY target/$($JAR_FILE.Name) app.jar

# Exponer puerto (Spring Boot default)
EXPOSE 8080

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
"@

    # Guardar Dockerfile en el directorio del servicio
    $DOCKERFILE_CONTENT | Out-File -FilePath "$SERVICE\Dockerfile.simple" -Encoding utf8
    
    Write-Host "[$SERVICE] Construyendo imagen Docker..." -ForegroundColor Yellow
    
    # Build desde el directorio del servicio (contexto limitado)
    docker build -t "${ACR_SERVER}/${SERVICE}:latest" -f "$SERVICE\Dockerfile.simple" "$SERVICE"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[$SERVICE] ERROR: Falló la construcción" -ForegroundColor Red
        continue
    }
    
    # Subir la imagen a ACR
    Write-Host "[$SERVICE] Subiendo a ACR..." -ForegroundColor Cyan
    docker push "${ACR_SERVER}/${SERVICE}:latest"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[$SERVICE] ERROR: Falló el push" -ForegroundColor Red
        continue
    }
    
    Write-Host "[$SERVICE] Completado exitosamente" -ForegroundColor Green
    Write-Host ""
}

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "Proceso finalizado" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Verificar imágenes en ACR:" -ForegroundColor Yellow
Write-Host "az acr repository list --name ecommercerojas435acr --output table" -ForegroundColor White
