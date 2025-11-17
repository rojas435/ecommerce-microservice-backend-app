# Script para migrar imágenes públicas de DockerHub a ACR
# Usa las imágenes de selimhorri que ya funcionan

$SERVICES = @(
    @{name="order-service"; image="selimhorri/order-service-ecommerce-boot:0.1.0"},
    @{name="payment-service"; image="selimhorri/payment-service-ecommerce-boot:0.1.0"},
    @{name="shipping-service"; image="selimhorri/shipping-service-ecommerce-boot:0.1.0"},
    @{name="product-service"; image="selimhorri/product-service-ecommerce-boot:0.1.0"},
    @{name="user-service"; image="selimhorri/user-service-ecommerce-boot:0.1.0"},
    @{name="favourite-service"; image="selimhorri/favourite-service-ecommerce-boot:0.1.0"},
    @{name="proxy-client"; image="selimhorri/proxy-client-ecommerce-boot:0.1.0"},
    @{name="api-gateway"; image="selimhorri/api-gateway-ecommerce-boot:0.1.0"},
    @{name="service-discovery"; image="selimhorri/service-discovery-ecommerce-boot:0.1.0"},
    @{name="cloud-config"; image="selimhorri/cloud-config-ecommerce-boot:0.1.0"}
)

$ACR_SERVER = "ecommercerojas435acr.azurecr.io"

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "Migrando imágenes de DockerHub a ACR" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

foreach ($service in $SERVICES) {
    Write-Host "[$($service.name)] Descargando imagen..." -ForegroundColor Cyan
    docker pull $service.image
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[$($service.name)] ERROR: Falló la descarga" -ForegroundColor Red
        continue
    }
    
    Write-Host "[$($service.name)] Re-etiquetando..." -ForegroundColor Yellow
    docker tag $service.image "${ACR_SERVER}/$($service.name):latest"
    
    Write-Host "[$($service.name)] Subiendo a ACR..." -ForegroundColor Cyan
    docker push "${ACR_SERVER}/$($service.name):latest"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[$($service.name)] ERROR: Falló el push" -ForegroundColor Red
        continue
    }
    
    Write-Host "[$($service.name)] Completado" -ForegroundColor Green
    Write-Host ""
}

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "Verificando imágenes en ACR..." -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
az acr repository list --name ecommercerojas435acr --output table
