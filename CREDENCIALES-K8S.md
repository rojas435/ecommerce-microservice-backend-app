# 🔐 Credenciales y Accesos - Kubernetes

## ✅ Estado del Cluster

**Minikube configuración:**
- RAM: **18GB** (20GB en WSL2)
- CPUs: **6**
- Disk: **50GB**

## 🔧 Jenkins

**URL:** http://localhost:8081

**Credenciales:**
- Usuario: `admin`
- Password: `YxQhoLI91vvVsyC8UVJTid`

**Port-forward:**
```powershell
kubectl port-forward -n jenkins svc/jenkins 8081:8080
```

**Nota:** Usamos puerto 8081 local porque el 8080 está ocupado por pgAdmin

**Verificar estado:**
```powershell
kubectl get pods -n jenkins
```

## 🔍 SonarQube

**URL:** http://localhost:9001

**Credenciales iniciales:**
- Usuario: `admin`
- Password: `admin123` (cambiar en primer login)

**Port-forward:**
```powershell
kubectl port-forward -n sonarqube svc/sonarqube 9001:9000
```

**Nota:** Usamos puerto 9001 local para evitar conflictos

**Verificar estado:**
```powershell
kubectl get pods -n sonarqube
```

## 🚀 Microservicios

**Namespace:** `ecommerce`

**Servicios desplegados:**
- api-gateway (puerto 8080)
- order-service (puerto 8080)
- product-service (puerto 8080)
- user-service (puerto 8080)
- favourite-service (puerto 8080)
- shipping-service (puerto 8080)
- payment-service (puerto 8080)

**Verificar estado:**
```powershell
kubectl get pods -n ecommerce
kubectl get svc -n ecommerce
```

## 📋 Próximos Pasos

### 1. Configurar SonarQube
1. Accede a http://localhost:9000
2. Login con admin/admin123
3. Cambia la password a algo más seguro (ej: `admin123`)
4. Ve a **Administration** → **Security** → **Users** → **Tokens**
5. Genera un token nuevo para Jenkins:
   - Name: `jenkins-token`
   - Type: `Global Analysis Token`
   - Expiration: `No expiration`
6. **COPIA EL TOKEN** (lo necesitarás para Jenkins)

### 2. Configurar Jenkins
1. Accede a http://localhost:8080
2. Login con admin/YxQhoLI91vvVsyC8UVJTid
3. Ve a **Manage Jenkins** → **Credentials** → **System** → **Global credentials**
4. Click **Add Credentials**:
   - Kind: `Secret text`
   - Secret: `<el-token-de-sonarqube>`
   - ID: `sonar-token`
   - Description: `SonarQube Token`
5. Ve a **Manage Jenkins** → **System** → busca **SonarQube servers**
6. Verifica que esté configurado:
   - Name: `SonarQube`
   - Server URL: `http://sonarqube.sonarqube.svc.cluster.local:9000`
   - Server authentication token: `sonar-token`

### 3. Verificar Kubernetes Cloud en Jenkins
1. Ve a **Manage Jenkins** → **Clouds** → **Kubernetes**
2. Verifica que esté configurado con:
   - Kubernetes URL: `https://kubernetes.default.svc.cluster.local`
   - Jenkins URL: `http://jenkins.jenkins.svc.cluster.local:8080`
   - Pod Templates configurados

### 4. Crear Pipeline
1. En Jenkins, click **New Item**
2. Nombre: `ecommerce-pipeline`
3. Tipo: **Multibranch Pipeline**
4. Configurar:
   - Branch Sources: **Git**
   - Project Repository: `<tu-repo-url>`
   - Credentials: (agregar si es privado)
   - Behaviors: Discover branches
   - Script Path: `Jenkinsfile`
5. Guardar y ejecutar **Scan Multibranch Pipeline Now**

## 🔧 Comandos Útiles

### Ver logs
```powershell
# Jenkins
kubectl logs -n jenkins jenkins-0 -c jenkins -f

# SonarQube
kubectl logs -n sonarqube -l app=sonarqube -f

# Microservicios
kubectl logs -n ecommerce -l app=api-gateway -f
```

### Reiniciar servicios
```powershell
# Reiniciar Jenkins
kubectl delete pod jenkins-0 -n jenkins

# Reiniciar SonarQube
kubectl rollout restart deployment sonarqube -n sonarqube

# Reiniciar un microservicio
kubectl rollout restart deployment api-gateway -n ecommerce
```

### Verificar recursos
```powershell
kubectl top node
kubectl top pods -n jenkins
kubectl top pods -n sonarqube
kubectl top pods -n ecommerce
```

## 🆘 Troubleshooting

### Si Jenkins está lento
```powershell
kubectl exec -n jenkins jenkins-0 -c jenkins -- free -h
kubectl exec -n jenkins jenkins-0 -c jenkins -- ps aux --sort=-%mem | head
```

### Si SonarQube no arranca
```powershell
kubectl describe pod -n sonarqube -l app=sonarqube
kubectl logs -n sonarqube -l app=sonarqube --previous
```

### Si los port-forwards se caen
```powershell
# Detener todos los port-forwards
Get-Process | Where-Object { $_.ProcessName -eq "kubectl" -and $_.CommandLine -like "*port-forward*" } | Stop-Process -Force

# Reiniciar
kubectl port-forward -n jenkins svc/jenkins 8080:8080 &
kubectl port-forward -n sonarqube svc/sonarqube 9000:9000 &
```
