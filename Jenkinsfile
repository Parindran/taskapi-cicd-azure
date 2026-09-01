pipeline {
    agent any

    // Names must match Manage Jenkins > Tools exactly.
    // Using tools{} means the build does not depend on the Jenkins
    // service having Maven on its PATH.
    tools {
        jdk   'JDK-21'
        maven 'Maven-3.9.16'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '15'))
        timeout(time: 15, unit: 'MINUTES')
    }

    triggers {
        githubPush()
    }

    stages {
        // Jenkins checks out the repo automatically before this runs
        // when the job uses "Pipeline script from SCM".

        stage('Build') {
            steps {
                bat 'mvn -B -ntp clean compile'
            }
        }

        stage('Test') {
            steps {
                bat 'mvn -B -ntp test'
            }
            post {
                always {
                    // Publish results even when tests fail, so the
                    // report shows which case broke.
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                bat 'mvn -B -ntp package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }

    post {
        success {
            echo "Build ${env.BUILD_NUMBER} passed - jar archived."
        }
        failure {
            echo "Build ${env.BUILD_NUMBER} failed - check the Test Result report."
        }
    }
}
