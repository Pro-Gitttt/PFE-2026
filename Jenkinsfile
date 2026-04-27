pipeline {
    agent any

    parameters {
        string(name: 'EXECUTION_ID', defaultValue: '1')
        string(name: 'PROJECT_ID', defaultValue: '1')
    }

    environment {

        // JAVA
        JDK21 = "/usr/lib/jvm/java-21-openjdk-amd64"
        JDK17 = "/usr/lib/jvm/java-17-openjdk-amd64"

        // SONAR
        SONAR_URL = "http://192.168.40.128:9000"
        SONAR_TOKEN = "sqa_4028d3afe1c221d943755d9e5123c8b91f770d9b"

        // SECURITY SERVICE (ONLY ONE SOURCE OF TRUTH)
        SECURITY_SERVICE_URL = "http://192.168.1.10:8083/api/security/scan"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                withEnv(["JAVA_HOME=${JDK21}", "PATH+JAVA=${JDK21}/bin"]) {
                    sh '''
                    java -version
                    mvn clean verify -DskipTests=false
                    '''
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withEnv(["JAVA_HOME=${JDK17}", "PATH+JAVA=${JDK17}/bin"]) {
                    sh """
                    mvn sonar:sonar \
                      -Dsonar.projectKey=devsecops-project \
                      -Dsonar.host.url=${SONAR_URL} \
                      -Dsonar.login=${SONAR_TOKEN}
                    """
                }
            }
        }

        stage('Trivy Scan') {
            steps {
                sh '''
                trivy fs --format json -o trivy.json . || true
                '''
            }
        }

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

        stage('Send Reports') {
            steps {
                script {

                    // safety: avoid missing files
                    sh '''
                    test -f trivy.json || echo "{}" > trivy.json
                    test -f gitleaks.json || echo "{}" > gitleaks.json
                    '''

                    echo "Sending to: ${SECURITY_SERVICE_URL}"

                    def response = sh(script: """
                    curl -s -X POST ${SECURITY_SERVICE_URL} \
                      -F executionId=${EXECUTION_ID} \
                      -F projectId=${PROJECT_ID} \
                      -F trivy=@trivy.json \
                      -F gitleaks=@gitleaks.json
                    """, returnStdout: true).trim()

                    echo "Response: ${response}"

                    if (response.contains('"blocked":true')) {
                        error("❌ SECURITY BLOCKED PIPELINE")
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
            echo "✅ PIPELINE SUCCESS"
        }

        failure {
            echo "❌ PIPELINE FAILED"
        }
    }
}