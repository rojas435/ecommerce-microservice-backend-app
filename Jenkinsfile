pipeline {
  agent {
    kubernetes {
      yaml '''
        apiVersion: v1
        kind: Pod
        metadata:
          namespace: ecommerce
        spec:
          serviceAccountName: jenkins-agent
          containers:
          - name: maven
            image: maven:3.9.9-eclipse-temurin-17
            command: ['sleep']
            args: ['99d']
            resources:
              requests:
                memory: "1Gi"
                cpu: "500m"
              limits:
                memory: "2Gi"
                cpu: "1000m"
          - name: kubectl
            image: alpine/k8s:1.28.3
            command: ['cat']
            tty: true
            resources:
              requests:
                memory: "128Mi"
                cpu: "100m"
              limits:
                memory: "256Mi"
                cpu: "200m"
      '''
    }
  }
  options {
    timeout(time: 30, unit: 'MINUTES')
  }
  parameters {
    booleanParam(name: 'RUN_E2E', defaultValue: false, description: 'Ejecutar e2e-tests al final')
    booleanParam(name: 'RUN_PERFORMANCE_TESTS', defaultValue: false, description: 'Ejecutar performance tests con Locust')
  }
  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build & Unit Tests (parallel)') {
      parallel {
        stage('order-service') {
          steps {
            container('maven') {
              dir('order-service') {
                sh 'mvn -B -U clean package -Dtest="!**/*IntegrationTest" -DfailIfNoTests=false'
              }
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
            container('maven') {
              dir('payment-service') {
                sh 'mvn -B -U clean package -Dtest="!**/*IntegrationTest" -DfailIfNoTests=false'
              }
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
            container('maven') {
              dir('shipping-service') {
                sh 'mvn -B -U clean package -Dtest="!**/*IntegrationTest" -DfailIfNoTests=false'
              }
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
        container('maven') {
          withSonarQubeEnv('SonarQube') {
            sh '''
              mvn -B -U \
                -pl order-service,payment-service,shipping-service -am \
                -Dtest="!**/*IntegrationTest" -DfailIfNoTests=false \
                test org.jacoco:jacoco-maven-plugin:0.8.8:report sonar:sonar
            '''
          }
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
            echo "Dashboard: http://sonarqube.sonarqube.svc.cluster.local:9000/dashboard?id=com.selimhorri%3Aecommerce-microservice-backend"
          }
        }
      }
    }

    stage('E2E (optional)') {
      when { expression { return params.RUN_E2E } }
      steps {
        container('maven') {
          dir('e2e-tests') {
            sh 'mvn -B -U clean test'
          }
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'e2e-tests/target/surefire-reports/*.xml'
        }
      }
    }

    stage('Performance Tests (optional)') {
      when { expression { return params.RUN_PERFORMANCE_TESTS } }
      steps {
        container('kubectl') {
          script {
            echo '=== Starting Performance Tests with Locust ==='
            
            // Create ConfigMap with locustfile.py
            sh '''
              kubectl create configmap locustfile \
                --from-file=locustfile.py \
                --namespace=ecommerce \
                --dry-run=client -o yaml | kubectl apply -f -
            '''
            
            // Delete previous job if exists
            sh 'kubectl delete job locust-performance-test -n ecommerce --ignore-not-found=true'
            sh 'sleep 5'
            
            // Apply performance test job
            sh 'kubectl apply -f k8s/performance-test-job.yaml'
            
            // Wait for job to complete (max 10 minutes)
            sh '''
              kubectl wait --for=condition=complete \
                --timeout=600s \
                job/locust-performance-test -n ecommerce || true
            '''
            
            // Get job status
            def jobStatus = sh(
              script: 'kubectl get job locust-performance-test -n ecommerce -o jsonpath=\'{.status.conditions[0].type}\'',
              returnStdout: true
            ).trim()
            
            echo "Job status: ${jobStatus}"
            
            // Get pod name
            def podName = sh(
              script: 'kubectl get pods -n ecommerce -l job-name=locust-performance-test -o jsonpath=\'{.items[0].metadata.name}\'',
              returnStdout: true
            ).trim()
            
            echo "Pod name: ${podName}"
            
            // Print logs
            sh "kubectl logs -n ecommerce ${podName} || true"
            
            // Extract HTML report
            sh """
              kubectl cp ecommerce/${podName}:/reports/performance-report.html \
                ./performance-report.html || echo 'Failed to copy HTML report'
            """
            
            // Extract CSV report
            sh """
              kubectl cp ecommerce/${podName}:/reports/performance_stats.csv \
                ./performance_stats.csv || echo 'Failed to copy CSV report'
            """
            
            // Validate job completed successfully
            if (jobStatus != 'Complete') {
              error "Performance test job failed or timed out"
            }
            
            echo '=== Performance Tests Completed ==='
          }
        }
      }
      post {
        always {
          // Archive reports
          archiveArtifacts artifacts: 'performance-report.html,performance_stats.csv', allowEmptyArchive: true
          
          // Publish HTML report
          publishHTML([
            reportDir: '.',
            reportFiles: 'performance-report.html',
            reportName: 'Locust Performance Report',
            keepAll: true,
            alwaysLinkToLastBuild: true
          ])
        }
        cleanup {
          // Optional: Clean up job after extracting reports
          sh 'kubectl delete job locust-performance-test -n ecommerce --ignore-not-found=true || true'
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
