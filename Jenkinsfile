pipeline {
    agent any

    parameters {
        string(name: 'EXECUTION_ID', defaultValue: '1',   description: 'Pipeline execution ID from the backend')
        string(name: 'PROJECT_ID',   defaultValue: '1',   description: 'Project ID in the backend database')
        string(name: 'COMMIT_HASH',  defaultValue: 'HEAD', description: 'Git commit hash to build')
    }

    environment {
        JAVA_HOME_21  = "/usr/lib/jvm/java-21-openjdk-amd64"
        JAVA_HOME_17  = "/usr/lib/jvm/java-17-openjdk-amd64"

        // Docker registry (Nexus on Jenkins VM)
        REGISTRY      = "192.168.56.10:5000"

        // SonarQube (Jenkins VM)
        SONAR_URL     = "http://192.168.56.10:9000"

        // Security service — NodePort on Kubernetes VM (30083)
        SECURITY_SERVICE_URL = "http://192.168.56.20:30083/api/security/scan"

        // Backend API through Gateway NodePort (30080) on Kubernetes VM
        GATEWAY_URL   = "http://192.168.56.20:30080"

        // k8s manifests location on Jenkins workspace
        K8S_INFRA       = "/var/lib/jenkins/workspace/PFE-2026/k8s/infra"
        K8S_APPS        = "/var/lib/jenkins/workspace/PFE-2026/k8s/apps"
        K8S_MONITORING  = "/var/lib/jenkins/workspace/PFE-2026/k8s/monitoring"
    }

    stages {

        // ─────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // ─────────────────────────────────────────
        stage('Build & Test') {
            steps {
                withEnv(["JAVA_HOME=${JAVA_HOME_21}", "PATH+JAVA=${JAVA_HOME_21}/bin"]) {
                    sh '''
                        java -version
                        mvn clean verify -DskipTests=false
                    '''
                }
            }
        }

        // ─────────────────────────────────────────
        stage('SonarQube Analysis') {
            steps {
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                    withEnv(["JAVA_HOME=${JAVA_HOME_17}", "PATH+JAVA=${JAVA_HOME_17}/bin"]) {
                        sh """
                            mvn sonar:sonar \
                              -Dsonar.projectKey=PFE-2026 \
                              -Dsonar.host.url=${SONAR_URL} \
                              -Dsonar.login=${SONAR_TOKEN}
                        """
                    }
                }
            }
        }

        // ─────────────────────────────────────────
        stage('Trivy Scan') {
            steps {
                sh '''
                    trivy fs \
                      --format json \
                      --output ${WORKSPACE}/trivy.json \
                      --ignore-unfixed \
                      --severity CRITICAL,HIGH \
                      --skip-dirs "frontend/node_modules,frontend/.angular,frontend/dist" \
                      --skip-dirs "**/target,**/target/**" \
                      --ignorefile ${WORKSPACE}/.trivyignore \
                      . 2>/dev/null || true
                    if [ ! -s ${WORKSPACE}/trivy.json ]; then echo '{"Results":[]}' > ${WORKSPACE}/trivy.json; fi
                    echo "trivy.json: $(wc -c < ${WORKSPACE}/trivy.json) bytes"
                '''
            }
        }

        // ─────────────────────────────────────────
        stage('Gitleaks Scan') {
            steps {
                sh '''
                    gitleaks detect \
                      --source . \
                      --config ${WORKSPACE}/.gitleaks.toml \
                      --report-format json \
                      --report-path ${WORKSPACE}/gitleaks.json 2>/dev/null || true
                    if [ ! -s ${WORKSPACE}/gitleaks.json ]; then echo '[]' > ${WORKSPACE}/gitleaks.json; fi
                    echo "gitleaks.json: $(wc -c < ${WORKSPACE}/gitleaks.json) bytes"
                '''
            }
        }

        // ─────────────────────────────────────────
        stage('Send Security Reports') {
            steps {
                script {
                    echo "Sending security reports to: ${SECURITY_SERVICE_URL}"
                    echo "  executionId = ${EXECUTION_ID}"
                    echo "  projectId   = ${PROJECT_ID}"

                    def response = sh(script: """
                        curl -sf -X POST ${SECURITY_SERVICE_URL} \
                          -F "executionId=${EXECUTION_ID}" \
                          -F "projectId=${PROJECT_ID}" \
                          -F "trivy=@${WORKSPACE}/trivy.json" \
                          -F "gitleaks=@${WORKSPACE}/gitleaks.json"
                    """, returnStdout: true).trim()

                    echo "Security Service Response: ${response}"

                    if (response.contains('"blocked":true')) {
                        // Audit the block event
                        sh """
                            curl -sf -X POST ${GATEWAY_URL}/api/audit/log \
                              -H 'Content-Type: application/json' \
                              -d '{
                                "action":        "SECURITY_SCAN_BLOCKED",
                                "resource":      "PIPELINE",
                                "resourceId":    ${EXECUTION_ID},
                                "details":       "Pipeline execution ${EXECUTION_ID} blocked — security score below threshold or too many critical vulnerabilities",
                                "status":        "FAILURE",
                                "sourceService": "jenkins"
                              }' || true
                        """
                        // Update execution status to BLOCKED
                        sh """
                            curl -sf -X PUT ${GATEWAY_URL}/api/executions/${EXECUTION_ID}/status \
                              -H 'Content-Type: application/json' \
                              -d '{"status":"BLOCKED"}' || true
                        """
                        error("❌ PIPELINE BLOCKED — Security vulnerabilities detected. Fix them and retry.")
                    } else {
                        // Emit audit event for passed scan
                        sh """
                            curl -sf -X POST ${GATEWAY_URL}/api/audit/log \
                              -H 'Content-Type: application/json' \
                              -d '{
                                "action":        "SECURITY_SCAN_COMPLETED",
                                "resource":      "PIPELINE",
                                "resourceId":    ${EXECUTION_ID},
                                "details":       "Security scan passed for execution ${EXECUTION_ID}",
                                "status":        "SUCCESS",
                                "sourceService": "jenkins"
                              }' || true
                        """
                    }
                }
            }
        }

        // ─────────────────────────────────────────
        stage('Docker Build') {
            steps {
                withEnv(["JAVA_HOME=${JAVA_HOME_21}", "PATH+JAVA=${JAVA_HOME_21}/bin"]) {
                    sh """
                        docker build -t ${REGISTRY}/eureka-server:latest       ./Eureka-Server/
                        docker build -t ${REGISTRY}/api-gateway:latest          ./Gateway/
                        docker build -t ${REGISTRY}/auth-service:latest         ./Auth-Service/
                        docker build -t ${REGISTRY}/pipeline-service:latest     ./Pipeline-Service/
                        docker build -t ${REGISTRY}/security-service:latest     ./SECURITY-SERVICE/
                        docker build -t ${REGISTRY}/notification-service:latest ./Notification-Service/
                        docker build -t ${REGISTRY}/audit-log-service:latest    ./Audit-Log-Service/
                    """
                }
            }
        }

        // ─────────────────────────────────────────
        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-credentials',
                    usernameVariable: 'NEXUS_USER',
                    passwordVariable: 'NEXUS_PASS'
                )]) {
                    sh """
                        echo "${NEXUS_PASS}" | docker login ${REGISTRY} \
                          -u ${NEXUS_USER} --password-stdin

                        docker push ${REGISTRY}/eureka-server:latest
                        docker push ${REGISTRY}/api-gateway:latest
                        docker push ${REGISTRY}/auth-service:latest
                        docker push ${REGISTRY}/pipeline-service:latest
                        docker push ${REGISTRY}/security-service:latest
                        docker push ${REGISTRY}/notification-service:latest
                        docker push ${REGISTRY}/audit-log-service:latest
                    """
                }
            }
        }

        // ─────────────────────────────────────────
        stage('Deploy to Kubernetes') {
            steps {
                sh """
                    # Apply infra first
                    kubectl apply -f ${K8S_INFRA}/mysql.yaml
                    kubectl apply -f ${K8S_INFRA}/eureka.yaml
                    kubectl apply -f ${K8S_INFRA}/gateway.yaml

                    # Apply app services
                    kubectl apply -f ${K8S_APPS}/auth.yaml
                    kubectl apply -f ${K8S_APPS}/pipeline.yaml
                    kubectl apply -f ${K8S_APPS}/security.yaml
                    kubectl apply -f ${K8S_APPS}/notification.yaml
                    kubectl apply -f ${K8S_APPS}/audit.yaml

                    # Monitoring
                    kubectl apply -f ${K8S_MONITORING}/prometheus.yaml
                    kubectl apply -f ${K8S_MONITORING}/grafana.yaml

                    # Force rollout to pick up new :latest images
                    kubectl rollout restart deployment/eureka-server      -n infra
                    kubectl rollout restart deployment/api-gateway         -n infra
                    kubectl rollout restart deployment/auth-service        -n apps
                    kubectl rollout restart deployment/pipeline-service    -n apps
                    kubectl rollout restart deployment/security-service    -n apps
                    kubectl rollout restart deployment/notification-service -n apps
                    kubectl rollout restart deployment/audit-log-service   -n apps

                    # Wait for rollouts to complete
                    kubectl rollout status deployment/api-gateway          -n infra --timeout=120s
                    kubectl rollout status deployment/pipeline-service     -n apps  --timeout=120s
                    kubectl rollout status deployment/security-service     -n apps  --timeout=120s
                    kubectl rollout status deployment/audit-log-service    -n apps  --timeout=120s
                """
            }
        }

        // ─────────────────────────────────────────
        stage('Verify Deployment') {
            steps {
                sh '''
                    echo "=== INFRA PODS ==="
                    kubectl get pods -n infra

                    echo "=== APPS PODS ==="
                    kubectl get pods -n apps

                    echo "=== MONITORING PODS ==="
                    kubectl get pods -n monitoring

                    echo "=== SERVICES ==="
                    kubectl get svc -n infra
                    kubectl get svc -n apps
                '''
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '*.json', fingerprint: true
        }
        success {
            echo "✅ PIPELINE SUCCESS — All services deployed!"
            script {
                sh """
                    curl -sf -X PUT ${GATEWAY_URL}/api/executions/${EXECUTION_ID}/status \
                      -H 'Content-Type: application/json' \
                      -d '{"status":"SUCCESS"}' || true

                    curl -sf -X POST ${GATEWAY_URL}/api/audit/log \
                      -H 'Content-Type: application/json' \
                      -d '{
                        "action":        "PIPELINE_TRIGGERED",
                        "resource":      "PIPELINE",
                        "resourceId":    ${EXECUTION_ID},
                        "details":       "Pipeline execution ${EXECUTION_ID} deployed successfully",
                        "status":        "SUCCESS",
                        "sourceService": "jenkins"
                      }' || true
                """
            }
        }
        failure {
            echo "❌ PIPELINE FAILED"
            script {
                sh """
                    curl -sf -X PUT ${GATEWAY_URL}/api/executions/${EXECUTION_ID}/status \
                      -H 'Content-Type: application/json' \
                      -d '{"status":"FAILED"}' || true

                    curl -sf -X POST ${GATEWAY_URL}/api/audit/log \
                      -H 'Content-Type: application/json' \
                      -d '{
                        "action":        "PIPELINE_ABORTED",
                        "resource":      "PIPELINE",
                        "resourceId":    ${EXECUTION_ID},
                        "details":       "Pipeline execution ${EXECUTION_ID} failed",
                        "status":        "FAILURE",
                        "sourceService": "jenkins"
                      }' || true
                """
            }
        }
    }
}
