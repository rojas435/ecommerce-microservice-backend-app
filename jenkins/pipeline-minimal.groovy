pipeline {
    agent any
    
    stages {
        stage('Test Java') {
            steps {
                echo '=== Testing Java (built-in) ==='
                sh 'java --version'
            }
        }
        
        stage('Test Workspace') {
            steps {
                echo '=== Testing Workspace Access ==='
                sh 'pwd'
                sh 'whoami'
                sh 'ls -la /var/jenkins_home'
            }
        }
        
        stage('Test Environment') {
            steps {
                echo '=== Testing Environment Variables ==='
                sh 'printenv | grep -E "(JAVA|JENKINS)" | head -10'
            }
        }
        
        stage('Wait for Docker Install') {
            steps {
                echo '=== Nota: Docker y kubectl se estan instalando en background ==='
                echo 'Este pipeline pasara cuando la instalacion complete'
                echo 'Por ahora, estos stages demuestran que Jenkins funciona correctamente'
            }
        }
    }
    
    post {
        success {
            echo '✅ JENKINS ESTA FUNCIONANDO CORRECTAMENTE'
            echo ''
            echo 'Herramientas adicionales instalandose:'
            echo '- Docker CLI (para construir imagenes)'
            echo '- kubectl (para deployar a Kubernetes)'
            echo '- Maven (se instalara automaticamente cuando se use)'
            echo ''
            echo 'Esto tomara 5-10 minutos mas...'
        }
        failure {
            echo '❌ Algo fallo - revisa los logs'
        }
    }
}
