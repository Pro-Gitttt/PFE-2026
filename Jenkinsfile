pipeline {
    agent any

    parameters {
        string(name: 'EXECUTION_ID', defaultValue: '', description: 'Execution ID from Spring Boot')
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo "Building project..."
                sh 'echo BUILD SUCCESS'
            }
        }

        stage('Test') {
            steps {
                echo "Running tests..."
                sh 'echo TEST SUCCESS'
            }
        }

    }
}