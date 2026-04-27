pipeline {
    agent any

    parameters {
        string(name: 'EXECUTION_ID', defaultValue: '1')
        string(name: 'PROJECT_ID', defaultValue: '1')
    }

environment {

    // =========================
    // JAVA
    // =========================
    JDK21 = "/usr/lib/jvm/java-21-openjdk-amd64"
    JDK17 = "/usr/lib/jvm/java-17-openjdk-amd64"

    // =========================
    // SONAR
    // =========================
    SONAR_URL = "http://192.168.40.128:9000"
    SONAR_TOKEN = "sqa_4028d3afe1c221d943755d9e5123c8b91f770d9b"

    // =========================
    // SECURITY SERVICE (ADD THIS)
    // =========================
     SECURITY_SERVICE_URL = "${SECURITY_SERVICE_URL}"

}
    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // =========================
        // BUILD (JDK 21)
        // =========================
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

        // =========================
        // SONAR (JDK 17)
        // =========================
        stage('SonarQube Analysis') {
            steps {
                withEnv(["JAVA_HOME=${JDK17}", "PATH+JAVA=${JDK17}/bin"]) {
                    sh '''
                    mvn sonar:sonar \
                      -Dsonar.projectKey=devsecops-project \
                      -Dsonar.host.url=$SONAR_URL \
                      -Dsonar.login=$SONAR_TOKEN
                    '''
                }
            }
        }

        // =========================
        // TRIVY
        // =========================
        stage('Trivy Scan') {
            steps {
                sh '''
                trivy fs --format json -o trivy.json . || true
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
        // SEND TO SECURITY SERVICE
        // =========================
        stage('Send Reports') {
            steps {
                script {

                    // safety fallback (avoid null errors)
                    sh '''
                    test -f trivy.json || echo "{}" > trivy.json
                    test -f gitleaks.json || echo "{}" > gitleaks.json
                    '''

                    def response = sh(script: """
                    curl -s -X POST ${SECURITY_SERVICE_URL} \
                      -F executionId=${EXECUTION_ID} \
                      -F projectId=${PROJECT_ID} \
                      -F trivy=@trivy.json \
                      -F gitleaks=@gitleaks.json
                    """, returnStdout: true).trim()

                    echo "Security Response: ${response}"

                    if (response.contains('"blocked":true')) {
                        error("❌ BLOCKED by Security Service")
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
            echo "✅ Pipeline SUCCESS"
        }

        failure {
            echo "❌ Pipeline FAILED"
        }
    }
}