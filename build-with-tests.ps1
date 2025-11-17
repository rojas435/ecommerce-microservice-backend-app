# Script para construir TODAS las imágenes localmente con el parent POM correcto
# Esto incluirá TUS pruebas y generará coverage real para SonarQube

$ACR_SERVER = "ecommercerojas435acr.azurecr.io"
$PROJECT_ROOT = Get-Location

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "CONSTRUYENDO IMÁGENES PROPIAS CON PRUEBAS" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

# Primero, compilar el parent POM para instalar dependencias
Write-Host "[ROOT] Compilando parent POM..." -ForegroundColor Yellow
mvn clean install -DskipTests -f pom.xml

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ROOT] ERROR: Falló la compilación del parent POM" -ForegroundColor Red
    exit 1
}

Write-Host "[ROOT] Parent POM compilado exitosamente" -ForegroundColor Green
Write-Host ""

# Servicios a construir
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

foreach ($SERVICE in $SERVICES) {
    Write-Host "[$SERVICE] ====================================" -ForegroundColor Cyan
    
    $SERVICE_DIR = Join-Path $PROJECT_ROOT $SERVICE
    
    if (-Not (Test-Path $SERVICE_DIR)) {
        Write-Host "[$SERVICE] ERROR: Directorio no encontrado" -ForegroundColor Red
        continue
    }
    
    # Crear Dockerfile mejorado que usa el parent POM
    $DOCKERFILE_CONTENT = @"
# Multi-stage Dockerfile para $SERVICE
# Usa parent POM para resolver dependencias correctamente

FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

# Copiar parent POM primero
COPY pom.xml /workspace/pom.xml

# Copiar POM del servicio
COPY $SERVICE/pom.xml /workspace/$SERVICE/pom.xml

# Descargar dependencias del parent y del servicio
WORKDIR /workspace
RUN mvn dependency:go-offline -B -pl $SERVICE -am

# Copiar código fuente del servicio
COPY $SERVICE/src /workspace/$SERVICE/src

# Compilar servicio CON pruebas para generar coverage
WORKDIR /workspace/$SERVICE
RUN mvn clean package -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copiar JAR compilado
COPY --from=builder /workspace/$SERVICE/target/*.jar app.jar

RUN chown spring:spring app.jar

USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
"@

    # Guardar Dockerfile temporal
    $TEMP_DOCKERFILE = Join-Path $PROJECT_ROOT "Dockerfile.build.$SERVICE"
    $DOCKERFILE_CONTENT | Set-Content $TEMP_DOCKERFILE
    
    Write-Host "[$SERVICE] Construyendo imagen Docker..." -ForegroundColor Yellow
    
    # Construir desde la raíz del proyecto (para tener acceso al parent POM)
    docker build `
        -f $TEMP_DOCKERFILE `
        -t "${ACR_SERVER}/${SERVICE}:latest" `
        -t "${ACR_SERVER}/${SERVICE}:$((Get-Date).ToString('yyyyMMdd-HHmmss'))" `
        .
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[$SERVICE] Imagen construida exitosamente" -ForegroundColor Green
        
        # Subir a ACR
        Write-Host "[$SERVICE] Subiendo a ACR..." -ForegroundColor Cyan
        docker push "${ACR_SERVER}/${SERVICE}:latest"
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[$SERVICE] Subido a ACR exitosamente" -ForegroundColor Green
        } else {
            Write-Host "[$SERVICE] ERROR: Falló el push a ACR" -ForegroundColor Red
        }
    } else {
        Write-Host "[$SERVICE] ERROR: Falló la construcción de Docker" -ForegroundColor Red
    }
    
    # Limpiar Dockerfile temporal
    Remove-Item $TEMP_DOCKERFILE -ErrorAction SilentlyContinue
    
    Write-Host ""
}

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "RESUMEN" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Verificar imágenes en ACR:" -ForegroundColor Yellow
az acr repository list --name ecommercerojas435acr --output table
