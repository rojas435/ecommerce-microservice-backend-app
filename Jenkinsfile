pipeline {
  agent any
  options {
    ansiColor('xterm')
    timestamps()
    timeout(time: 30, unit: 'MINUTES')
  }
  parameters {
    booleanParam(name: 'RUN_E2E', defaultValue: false, description: 'Ejecutar e2e-tests al final')
  }
  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build & Unit Tests (parallel)') {
      parallel {
        stage('order-service') {
          steps {
            dir('order-service') {
              sh 'mvn -B -U -DskipTests=false clean package'
            }
          }
          post {
            always {
              junit allowEmptyResults: true, testResults: 'order-service/**/target/surefire-reports/*.xml'
              archiveArtifacts artifacts: 'order-service/target/*.jar,order-service/target/*.jar.original', allowEmptyArchive: true
            }
          }
        }
        stage('payment-service') {
          steps {
            dir('payment-service') {
              sh 'mvn -B -U -DskipTests=false clean package'
            }
          }
          post {
            always {
              junit allowEmptyResults: true, testResults: 'payment-service/**/target/surefire-reports/*.xml'
              archiveArtifacts artifacts: 'payment-service/target/*.jar,payment-service/target/*.jar.original', allowEmptyArchive: true
            }
          }
        }
        stage('shipping-service') {
          steps {
            dir('shipping-service') {
              sh 'mvn -B -U -DskipTests=false clean package'
            }
          }
          post {
            always {
              junit allowEmptyResults: true, testResults: 'shipping-service/**/target/surefire-reports/*.xml'
              archiveArtifacts artifacts: 'shipping-service/target/*.jar,shipping-service/target/*.jar.original', allowEmptyArchive: true
            }
          }
        }
      }
    }

    stage('SonarQube Analysis') {
      steps {
        withSonarQubeEnv('SonarQube') {
          sh '''
            mvn -B -U \
              -pl order-service,payment-service,shipping-service -am \
              -DskipTests=false \
              test org.jacoco:jacoco-maven-plugin:0.8.8:report sonar:sonar
          '''
        }
      }
    }

    stage('Quality Gate') {
      steps {
        script {
          try {
            timeout(time: 3, unit: 'MINUTES') {
              def qg = waitForQualityGate()
              if (qg.status != 'OK') {
                echo "Quality Gate status: ${qg.status}"
                echo "Quality Gate failed but continuing pipeline (non-blocking)"
              } else {
                echo "Quality Gate passed"
              }
            }
          } catch (Exception e) {
            echo "Quality Gate timeout or error: ${e.message}"
            echo "Continuing pipeline - check Sonar dashboard manually"
            echo "Dashboard: http://sonarqube:9000/dashboard?id=com.selimhorri%3Aecommerce-microservice-backend"
          }
        }
      }
    }

    stage('E2E (optional)') {
      when { expression { return params.RUN_E2E } }
      steps {
        dir('e2e-tests') {
          sh 'mvn -B -U clean test'
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'e2e-tests/target/surefire-reports/*.xml'
        }
      }
    }
  }
  post {
    always {
      echo "Build: ${env.BUILD_URL}"
    }
  }
}
