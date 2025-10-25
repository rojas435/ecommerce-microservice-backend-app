# Script para actualizar el despliegue de Kubernetes con configuración más tolerante

Write-Host "🔄 Eliminando despliegue actual..." -ForegroundColor Yellow
kubectl delete namespace ecommerce --ignore-not-found=true

Write-Host "⏳ Esperando a que se limpien los recursos..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host "🚀 Creando namespace..." -ForegroundColor Cyan
kubectl apply -f k8s/namespace.yaml

Write-Host "📝 Creando ConfigMaps..." -ForegroundColor Cyan
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/api-gateway-configmap.yaml

Write-Host "🎯 Desplegando servicios..." -ForegroundColor Cyan
kubectl apply -f k8s/order-service.yaml
Start-Sleep -Seconds 2
kubectl apply -f k8s/product-service.yaml
kubectl apply -f k8s/user-service.yaml
Start-Sleep -Seconds 2
kubectl apply -f k8s/favourite-service.yaml
kubectl apply -f k8s/shipping-service.yaml
kubectl apply -f k8s/payment-service.yaml
Start-Sleep -Seconds 2
kubectl apply -f k8s/api-gateway.yaml

Write-Host ""
Write-Host "✅ Despliegue iniciado!" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Estado de los pods:" -ForegroundColor Cyan
kubectl get pods -n ecommerce

Write-Host ""
Write-Host "🌐 Servicios disponibles:" -ForegroundColor Cyan
kubectl get services -n ecommerce

Write-Host ""
Write-Host "💡 Para ver el progreso en tiempo real:" -ForegroundColor Yellow
Write-Host "   kubectl get pods -n ecommerce -w" -ForegroundColor White
Write-Host ""
Write-Host "💡 Para ver logs de un servicio:" -ForegroundColor Yellow
Write-Host "   kubectl logs -f deployment/product-service -n ecommerce" -ForegroundColor White
Write-Host ""
Write-Host "💡 Espera 2-3 minutos y luego prueba los endpoints:" -ForegroundColor Yellow
Write-Host "   minikube service list -n ecommerce" -ForegroundColor White
Write-Host "   o accede directamente a: http://192.168.49.2:30080" -ForegroundColor White
