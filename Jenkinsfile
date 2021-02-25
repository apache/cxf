#!groovy

pipeline {
  agent none
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
            values 'jdk_1.8_latest', 'jdk_11_latest', 'jdk_15_latest'
          }
        }
        stages {
          stage('JDK specific build') {
            tools {
              jdk "${JAVA_VERSION}"
              
            }
            environment {
              MAVEN_OPTS = "-Xmx1024m"
            }
            stages {
              stage('Build & Test') {
                steps {
                  sh 'mvn -B clean install'
                  // step([$class: 'JiraIssueUpdater', issueSelector: [$class: 'DefaultIssueSelector'], scm: scm])
                }
               // post {
               //     always {
               //       junit(testResults: '**/surefire-reports/*.xml', allowEmptyResults: true)
               //       junit(testResults: '**/failsafe-reports/*.xml', allowEmptyResults: true)
               //     }
               // }
             }
          }
        }
      }
    }
  }
}
}
