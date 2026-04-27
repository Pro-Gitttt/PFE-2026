pipeline {
    agent any

    parameters {
        string(name: 'EXECUTION_ID', defaultValue: '1', description: 'Pipeline execution ID')
        string(name: 'PROJECT_ID', defaultValue: '1', description: 'Project ID')
    }

    environment {
        // =========================
        // JAVA PATHS
        // =========================
        JDK21 = "/usr/lib/jvm/java-21-openjdk-amd64"
        JDK17 = "/usr/lib/jvm/java-17-openjdk-amd64"

        // =========================
        // SONAR
        // =========================
        SONAR_URL   = "http://192.168.40.128:9000"
        SONAR_TOKEN = "sqa_4028d3afe1c221d943755d9e5123c8b91f770d9b"

        // =========================
        // SECURITY SERVICE
        // =========================
        SECURITY_SERVICE_URL = "http://192.168.40.128:8083/api/security/scan"
    }

    stages {

        // =========================
        // CHECKOUT
        // =========================
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // =========================
        // BUILD WITH JDK 21
        // =========================
        stage('Build & Test') {
            steps {
                withEnv([
                    "JAVA_HOME=${JDK21}",
                    "PATH=${JDK21}/bin:${env.PATH}"
                ]) {
                    sh '''
                    java -version
                    mvn clean verify -DskipTests=false
                    '''
                }
            }
        }

        // =========================
        // SONAR WITH JDK 17
        // =========================
        stage('SonarQube Analysis') {
            steps {
                withEnv([
                    "JAVA_HOME=${JDK17}",
                    "PATH=${JDK17}/bin:${env.PATH}"
                ]) {
                    sh """
                    java -version
                    mvn sonar:sonar \
                      -Dsonar.projectKey=devsecops-project \
                      -Dsonar.host.url=$SONAR_URL \
                      -Dsonar.login=$SONAR_TOKEN
                    """
                }
            }
        }

        // =========================
        // TRIVY
        // =========================
        stage('Trivy Scan') {
            steps {
                sh '''
                trivy fs \
                  --format json \
                  -o trivy.json . || true
                '''
            }
        }

        // =========================
        // GITLEAKS
        // =========================
        stage('Gitleaks Scan') {
            steps {
                sh '''
                gitleaks detect \
                  --source . \
                  --report-format json \
                  --report-path gitleaks.json || true
                '''
            }
        }

        // =========================
        // SEND REPORTS
        // =========================
        stage('Send Reports') {
            steps {
                script {
                    def response = sh(script: """
                    curl -s -X POST $SECURITY_SERVICE_URL \
                      -F executionId=$EXECUTION_ID \
                      -F projectId=$PROJECT_ID \
                      -F trivy=@trivy.json \
                      -F gitleaks=@gitleaks.json
                    """, returnStdout: true).trim()

                    echo "Security Response: ${response}"

                    if (response.contains('"blocked":true')) {
                        error("❌ BLOCKED by Security Policy")
                    }
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '*.json', fingerprint: true
        }

        success {
            echo '✅ Pipeline completed successfully'
        }

        failure {
            echo '❌ Pipeline failed'
        }
    }
}