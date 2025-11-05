# Script de Optimización - Reducir recursos

# 1. Parar servicios no esenciales
kubectl scale deployment --replicas=0 -n ecommerce favourite-service
kubectl scale deployment --replicas=0 -n ecommerce product-service
kubectl scale deployment --replicas=0 -n ecommerce user-service

# 2. Actualizar Jenkins con menos memoria
helm upgrade jenkins jenkins/jenkins --namespace jenkins `
  --reuse-values `
  --set controller.resources.limits.memory=1Gi `
  --set controller.resources.requests.memory=512Mi

# 3. Actualizar SonarQube con menos memoria
kubectl patch deployment sonarqube -n sonarqube -p '{
  "spec": {
    "template": {
      "spec": {
        "containers": [{
          "name": "sonarqube",
          "resources": {
            "requests": {"memory": "1Gi"},
            "limits": {"memory": "2Gi"}
          }
        }]
      }
    }
  }
}'

# 4. Reiniciar pods
kubectl delete pod jenkins-0 -n jenkins
kubectl delete pod -l app=sonarqube -n sonarqube
