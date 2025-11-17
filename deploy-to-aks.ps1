# Script para desplegar microservicios en AKS usando imágenes de ACR
# Este script actualiza los manifiestos y despliega en Azure

$ACR_SERVER = "ecommercerojas435acr.azurecr.io"
$NAMESPACE = "ecommerce"
$K8S_DIR = ".\k8s"

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "Desplegando microservicios en AKS" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

# Verificar conexión a AKS
Write-Host "Verificando conexión a AKS..." -ForegroundColor Yellow
kubectl cluster-info
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: No hay conexión con el cluster AKS" -ForegroundColor Red
    Write-Host "Ejecuta: az aks get-credentials --resource-group ecommerce-dev-rg --name dev-aks-cluster" -ForegroundColor Yellow
    exit 1
}
Write-Host ""

# Crear namespace
Write-Host "Creando namespace $NAMESPACE..." -ForegroundColor Cyan
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
Write-Host ""

# Crear ConfigMap
Write-Host "Aplicando ConfigMaps..." -ForegroundColor Cyan
if (Test-Path "$K8S_DIR\configmap.yaml") {
    kubectl apply -f "$K8S_DIR\configmap.yaml"
}
if (Test-Path "$K8S_DIR\api-gateway-configmap.yaml") {
    kubectl apply -f "$K8S_DIR\api-gateway-configmap.yaml"
}
Write-Host ""

# Lista de servicios a desplegar
$SERVICES = @(
    "service-discovery",
    "cloud-config",
    "api-gateway",
    "order-service",
    "payment-service",
    "product-service",
    "shipping-service",
    "user-service",
    "favourite-service"
)

# Actualizar y desplegar cada servicio
foreach ($SERVICE in $SERVICES) {
    $YAML_FILE = "$K8S_DIR\$SERVICE.yaml"
    
    if (-Not (Test-Path $YAML_FILE)) {
        Write-Host "[$SERVICE] ADVERTENCIA: Archivo $YAML_FILE no encontrado" -ForegroundColor Yellow
        continue
    }
    
    Write-Host "[$SERVICE] Procesando..." -ForegroundColor Cyan
    
    # Leer el contenido del archivo
    $content = Get-Content $YAML_FILE -Raw
    
    # Reemplazar la imagen de selimhorri con ACR
    $oldImage1 = "selimhorri/$SERVICE-ecommerce-boot:0.1.0"
    $newImage = "${ACR_SERVER}/${SERVICE}:latest"
    $content = $content -replace [regex]::Escape($oldImage1), $newImage
    
    # También manejar casos donde la imagen ya esté con rojas43529
    $oldImage2 = "rojas43529/$SERVICE"
    $content = $content -replace "$oldImage2[^\s]*", $newImage
    
    # Guardar temporalmente el archivo modificado
    $TEMP_FILE = "$K8S_DIR\$SERVICE.temp.yaml"
    $content | Set-Content $TEMP_FILE
    
    # Aplicar el manifiesto
    kubectl apply -f $TEMP_FILE
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[$SERVICE] Desplegado exitosamente" -ForegroundColor Green
    } else {
        Write-Host "[$SERVICE] ERROR: Falló el despliegue" -ForegroundColor Red
    }
    
    # Limpiar archivo temporal
    Remove-Item $TEMP_FILE -ErrorAction SilentlyContinue
    Write-Host ""
}

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "Estado del despliegue" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Deployments:" -ForegroundColor Yellow
kubectl get deployments -n $NAMESPACE

Write-Host ""
Write-Host "Pods:" -ForegroundColor Yellow
kubectl get pods -n $NAMESPACE

Write-Host ""
Write-Host "Services:" -ForegroundColor Yellow
kubectl get svc -n $NAMESPACE

Write-Host ""
Write-Host "Para ver logs de un pod:" -ForegroundColor Cyan
Write-Host "kubectl logs -f <pod-name> -n $NAMESPACE" -ForegroundColor White

Write-Host ""
Write-Host "Para exponer el API Gateway:" -ForegroundColor Cyan
Write-Host "kubectl patch svc api-gateway -n $NAMESPACE -p '{`"spec`": {`"type`": `"LoadBalancer`"}}'" -ForegroundColor White
