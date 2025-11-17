# Configuración de CI/CD Pipeline

Este proyecto incluye un pipeline completo de CI/CD con GitHub Actions que implementa:

## Etapas del Pipeline

### 1. Build & Test
- Compilación con Maven
- Ejecución de pruebas unitarias
- Generación de reportes JaCoCo
- Análisis estático con SonarCloud

### 2. Security Scanning
- Escaneo de vulnerabilidades con Trivy
- Análisis de dependencias con OWASP Dependency Check
- Reportes de seguridad en GitHub Security

### 3. Docker Build & Push
- Construcción de imágenes Docker
- Escaneo de imágenes con Trivy
- Push a Azure Container Registry (ACR)
- Versionado semántico automático

### 4. Deploy to AKS
- Despliegue automático a Azure Kubernetes Service
- Health checks y smoke tests
- Rollout progresivo

### 5. Notifications
- Notificaciones de estado del pipeline

## Secrets Requeridos en GitHub

Para que el pipeline funcione correctamente, debes configurar los siguientes secrets en:
**Settings → Secrets and variables → Actions → New repository secret**

### Secrets de Azure

1. **AZURE_CREDENTIALS** (JSON con Service Principal):
```json
{
  "clientId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "clientSecret": "your-client-secret",
  "subscriptionId": "63ae0822-33d0-4b64-a366-eec6f5f21650",
  "tenantId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

**Obtener estas credenciales:**
```bash
az ad sp create-for-rbac \
  --name "github-actions-ecommerce" \
  --role contributor \
  --scopes /subscriptions/63ae0822-33d0-4b64-a366-eec6f5f21650 \
  --sdk-auth
```

2. **ACR_USERNAME**: Usuario del Azure Container Registry
```bash
az acr credential show --name ecommercerojas435acr --query username -o tsv
```

3. **ACR_PASSWORD**: Password del ACR
```bash
az acr credential show --name ecommercerojas435acr --query passwords[0].value -o tsv
```

4. **ARM_CLIENT_ID**: Service Principal Client ID (mismo del JSON anterior)

5. **ARM_CLIENT_SECRET**: Service Principal Secret (mismo del JSON anterior)

6. **ARM_SUBSCRIPTION_ID**: `63ae0822-33d0-4b64-a366-eec6f5f21650`

7. **ARM_TENANT_ID**: Tu Tenant ID de Azure

### Secrets de SonarCloud

8. **SONAR_TOKEN**: Token de SonarCloud

**Obtener el token:**
1. Ve a https://sonarcloud.io
2. Sign in with GitHub
3. My Account → Security → Generate Token
4. Copia el token generado

**Configurar proyecto en SonarCloud:**
1. Import Organization from GitHub: `rojas435`
2. Analyze new project: `ecommerce-microservice-backend-app`
3. Copia el `projectKey` generado (debería ser: `rojas435_ecommerce-microservices`)

## Configuración de Environments en GitHub

Para despliegues con aprobación manual:

1. **Settings → Environments → New environment**
2. Crear 3 environments: `dev`, `stage`, `prod`
3. Para `prod`:
   - Enable "Required reviewers"
   - Agregar tu usuario como reviewer
   - Enable "Wait timer": 5 minutos

## Trigger del Pipeline

El pipeline se ejecuta automáticamente en:

- **Push** a `master` o `develop`
- **Pull Request** a `master` o `develop`
- **Manual** desde Actions → CI/CD Pipeline → Run workflow

## Verificar Resultados

### SonarCloud
- URL: https://sonarcloud.io/dashboard?id=rojas435_ecommerce-microservices
- Verás: Coverage, Code Smells, Bugs, Vulnerabilities

### GitHub Security
- Security → Code scanning alerts
- Verás resultados de Trivy y OWASP

### Artifacts
- Actions → Workflow run → Artifacts
- `jacoco-reports`: Reportes de coverage
- `maven-artifacts`: JARs compilados
- `owasp-report`: Reporte de vulnerabilidades

## Comandos Útiles

### Ejecutar análisis localmente (opcional)
```bash
# Tests con coverage
mvn clean test jacoco:report

# SonarCloud scan local
mvn sonar:sonar \
  -Dsonar.projectKey=rojas435_ecommerce-microservices \
  -Dsonar.organization=rojas435 \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.login=YOUR_SONAR_TOKEN
```

### Verificar imágenes en ACR
```bash
az acr repository list --name ecommercerojas435acr --output table
az acr repository show-tags --name ecommercerojas435acr --repository order-service
```

### Verificar deploy en AKS
```bash
kubectl get pods -n ecommerce-minimal
kubectl logs -f deployment/api-gateway -n ecommerce-minimal
```

## Flujo Completo

```
┌─────────────┐
│  Git Push   │
└──────┬──────┘
       │
       v
┌─────────────────────────────┐
│  Stage 1: Build & Test      │
│  - Maven compile            │
│  - Unit tests               │
│  - JaCoCo coverage          │
│  - SonarCloud analysis      │
└──────┬──────────────────────┘
       │
       v
┌─────────────────────────────┐
│  Stage 2: Security Scan     │
│  - Trivy filesystem scan    │
│  - OWASP dependency check   │
│  - Upload to GitHub Sec     │
└──────┬──────────────────────┘
       │
       v
┌─────────────────────────────┐
│  Stage 3: Docker Build      │
│  - Build images (6 svcs)    │
│  - Trivy image scan         │
│  - Push to ACR              │
│  - Tag: version, sha, latest│
└──────┬──────────────────────┘
       │
       v
┌─────────────────────────────┐
│  Stage 4: Deploy to AKS     │
│  - Apply k8s manifests      │
│  - Wait for rollout         │
│  - Smoke tests              │
└──────┬──────────────────────┘
       │
       v
┌─────────────────────────────┐
│  Stage 5: Notify            │
│  - Send status              │
└─────────────────────────────┘
```

## Badges para README

Agrega estos badges a tu README.md:

```markdown
![CI/CD Pipeline](https://github.com/rojas435/ecommerce-microservice-backend-app/actions/workflows/cicd-pipeline.yml/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=rojas435_ecommerce-microservices&metric=alert_status)](https://sonarcloud.io/dashboard?id=rojas435_ecommerce-microservices)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=rojas435_ecommerce-microservices&metric=coverage)](https://sonarcloud.io/dashboard?id=rojas435_ecommerce-microservices)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=rojas435_ecommerce-microservices&metric=security_rating)](https://sonarcloud.io/dashboard?id=rojas435_ecommerce-microservices)
```
