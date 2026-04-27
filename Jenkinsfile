pipeline {
    agent any

    parameters {
        string(name: 'EXECUTION_ID', defaultValue: '1')
        string(name: 'PROJECT_ID', defaultValue: '1')
    }

    environment {
        SONAR_URL = "http://192.168.40.128:9000"
        SONAR_TOKEN = "sqa_4028d3afe1c221d943755d9e5123c8b91f770d9b"

        SECURITY_SERVICE_URL = "http://192.168.40.128:8083/api/security/scan"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean verify -DskipTests=false'
            }
        }

        // =========================
        // 🔍 SONARQUBE
        // =========================
     stage('SonarQube Analysis') {
         steps {
             sh """
             mvn clean verify sonar:sonar \
             -Dsonar.projectKey=devsecops-project \
             -Dsonar.host.url=http://192.168.40.128:9000 \
             -Dsonar.login=$SONAR_TOKEN
             """
         }
     }
        // =========================
        // 🔐 TRIVY
        // =========================
        stage('Trivy Scan') {
            steps {
                sh 'trivy fs --format json -o trivy.json .'
            }
        }

        // =========================
        // 🔐 GITLEAKS
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
        // 📡 SEND TO SECURITY SERVICE
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

                    echo "Response: ${response}"

                    if (response.contains('"blocked":true')) {
                        error("❌ BLOCKED by Security")
                    }
                }
            }
        }
    }
}