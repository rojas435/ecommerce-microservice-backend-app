// Script de prueba para verificar que Jenkins puede crear agentes en Kubernetes
// Ejecutar este script en Jenkins -> Manage Jenkins -> Script Console

// Test 1: Verificar conexión a Kubernetes API
println "=== Test 1: Kubernetes API ==="
try {
    def cloud = Jenkins.instance.clouds.getByName("kubernetes")
    println "✅ Kubernetes cloud encontrado: ${cloud.name}"
    println "   Server URL: ${cloud.serverUrl}"
    println "   Namespace: ${cloud.namespace}"
} catch (Exception e) {
    println "❌ Error: ${e.message}"
}

println "\n=== Test 2: Pod Templates ==="
try {
    def cloud = Jenkins.instance.clouds.getByName("kubernetes")
    def templates = cloud.getTemplates()
    println "✅ Templates encontrados: ${templates.size()}"
    templates.each { template ->
        println "   - ${template.name} (label: ${template.label})"
        println "     Containers: ${template.containers.collect { it.name }.join(', ')}"
    }
} catch (Exception e) {
    println "❌ Error: ${e.message}"
}

println "\n=== Test 3: Service Account ==="
try {
    def cloud = Jenkins.instance.clouds.getByName("kubernetes")
    def template = cloud.getTemplates()[0]
    println "✅ Service Account: ${template.serviceAccount}"
} catch (Exception e) {
    println "❌ Error: ${e.message}"
}

println "\n=== Test 4: Crear pod de prueba ==="
println "Para probar la creación de un pod, crea un pipeline con este código:"
println """
pipeline {
    agent {
        kubernetes {
            label 'maven'
        }
    }
    stages {
        stage('Test') {
            steps {
                container('maven') {
                    sh 'mvn --version'
                }
                container('kubectl') {
                    sh 'kubectl version --client'
                }
            }
        }
    }
}
"""
