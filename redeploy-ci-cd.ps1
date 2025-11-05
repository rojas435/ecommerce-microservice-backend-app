# Script para redeployar Jenkins, SonarQube y microservicios en Minikube

Write-Host "=== Redeployando CI/CD en Minikube ===" -ForegroundColor Cyan
Write-Host ""

# 1. Crear namespaces
Write-Host "1. Creando namespaces..." -ForegroundColor Yellow
kubectl create namespace jenkins --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace sonarqube --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace ecommerce --dry-run=client -o yaml | kubectl apply -f -

Write-Host ""
Write-Host "2. Desplegando SonarQube..." -ForegroundColor Yellow
kubectl apply -f k8s/ci-cd/sonarqube-deployment.yaml

Write-Host ""
Write-Host "Esperando a que SonarQube esté listo (2-3 minutos)..." -ForegroundColor Yellow
Start-Sleep -Seconds 180

kubectl get pods -n sonarqube

Write-Host ""
Write-Host "3. Instalando Jenkins con Helm..." -ForegroundColor Yellow
helm repo add jenkins https://charts.jenkins.io
helm repo update

helm install jenkins jenkins/jenkins `
  -n jenkins `
  -f k8s/ci-cd/jenkins-values.yaml `
  --wait --timeout 10m

Write-Host ""
Write-Host "Esperando a que Jenkins esté listo (3-5 minutos)..." -ForegroundColor Yellow
Start-Sleep -Seconds 180

kubectl get pods -n jenkins

Write-Host ""
Write-Host "4. Obteniendo credenciales..." -ForegroundColor Yellow
Write-Host ""
Write-Host "=== JENKINS ===" -ForegroundColor Cyan
$jenkinsPassword = kubectl get secret -n jenkins jenkins -o jsonpath="{.data.jenkins-admin-password}" | ForEach-Object { [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($_)) }
Write-Host "URL: http://localhost:8080" -ForegroundColor White
Write-Host "Usuario: admin" -ForegroundColor White
Write-Host "Password: $jenkinsPassword" -ForegroundColor Green

Write-Host ""
Write-Host "=== SONARQUBE ===" -ForegroundColor Cyan
Write-Host "URL: http://localhost:9000" -ForegroundColor White
Write-Host "Usuario: admin" -ForegroundColor White
Write-Host "Password: admin123 (cambiar en primer login)" -ForegroundColor White

Write-Host ""
Write-Host "5. Desplegando microservicios..." -ForegroundColor Yellow
kubectl apply -f k8s/ --recursive

Write-Host ""
Write-Host "Esperando a que los microservicios estén listos..." -ForegroundColor Yellow
Start-Sleep -Seconds 60

kubectl get pods -n ecommerce

Write-Host ""
Write-Host "=== Deployment completo ===" -ForegroundColor Green
Write-Host ""
Write-Host "SIGUIENTE PASO: Iniciar port-forwards" -ForegroundColor Cyan
Write-Host "Jenkins:    kubectl port-forward -n jenkins svc/jenkins 8080:8080" -ForegroundColor White
Write-Host "SonarQube:  kubectl port-forward -n sonarqube svc/sonarqube 9000:9000" -ForegroundColor White
Write-Host ""
Write-Host "Luego configura el token de SonarQube en Jenkins (el anterior ya no sirve)." -ForegroundColor Yellow
