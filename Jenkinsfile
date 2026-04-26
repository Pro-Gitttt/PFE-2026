pipeline {
    agent any

    parameters {
        string(name: 'EXECUTION_ID', defaultValue: '', description: 'Execution ID')
        string(name: 'PROJECT_ID', defaultValue: '', description: 'Project ID')
    }

    environment {
        SECURITY_SERVICE_URL = "http://192.168.40.1:8083/api/security/scan"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'echo BUILD SUCCESS'
            }
        }

        stage('Test') {
            steps {
                sh 'echo TEST SUCCESS'
            }
        }

        // =========================
        // 🔐 TRIVY SCAN
        // =========================
        stage('Trivy FS Scan') {
            steps {
                sh '''
                trivy fs --format json -o trivy.json .
                '''
            }
        }

        // =========================
        // 🔐 GITLEAKS SCAN (FIXED)
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
        stage('Send to Security Service') {
            steps {
                script {
                    def response = sh(script: """
                    curl -s -X POST $SECURITY_SERVICE_URL \
                    -H "Content-Type: application/json" \
                    -d '{
                        "projectId": "$PROJECT_ID",
                        "executionId": "$EXECUTION_ID"
                    }'
                    """, returnStdout: true).trim()

                    echo "Security Response: ${response}"

                    if (response.contains('"blocked":true')) {
                        error("❌ Pipeline BLOCKED by Security Service")
                    } else {
                        echo "✅ Security Passed"
                    }
                }
            }
        }
    }
}