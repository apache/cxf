#!groovy

pipeline {
  agent any
  options {
    buildDiscarder logRotator(daysToKeepStr: '14', numToKeepStr: '10')
    timeout(140)
    disableConcurrentBuilds()
    skipStagesAfterUnstable()
    quietPeriod(30)
  }
  triggers {
    pollSCM 'H/15 * * * *'
  }
  stages {
    stage('Build') {
      matrix {
        axes {
          axis {
            name 'JAVA_VERSION'
            values  '11'
          }
        }
        stages {
          stage('JDK specific build') {
            tools {
              jdk "11"
              
            }
            stages {
              stage('Build & Test') {
                steps {
                    always {
                      junit(testResults: '**/surefire-reports/*.xml', allowEmptyResults: true)
                      junit(testResults: '**/failsafe-reports/*.xml', allowEmptyResults: true)
                    }
                }
             }
          }
        }
      }
    }
  }
}
}
