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
    stage('Prepare') {
      agent {
        label 'ubuntu'
      }
      stages {
        stage('Clean up') {
          steps {
            cleanWs deleteDirs: true, patterns: [[pattern: '**/target/**', type: 'INCLUDE']]
          }
        }
      }
    }
    stage('Build') {
      matrix {
        agent {
          label 'ubuntu'
        }
        axes {
          axis {
            name 'JAVA_VERSION'
            values 'jdk_1.8_latest', 'jdk_11_latest', 'jdk_15_latest'
          }
        }
        stages {
          stage('JDK specific build') {
            agent {
              label 'ubuntu'
            }
            tools {
              jdk "${JAVA_VERSION}"
              maven 'maven_latest'
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
                post {
                  always {
                    junit(testResults: '**/surefire-reports/*.xml', allowEmptyResults: true)
                    junit(testResults: '**/failsafe-reports/*.xml', allowEmptyResults: true)
                  }
                }
              }
              /* stage('Build Source & JavaDoc') {
              when {
                branch 'master'
              }
              steps {
                dir("local-snapshots-dir/") {
                  deleteDir()
                }
                sh 'mvn -B source:jar javadoc:jar -DskipAssembbly'
              }
            }
            stage('Deploy Snapshot') {
              when {
                branch 'master'
              }
              steps {
                withCredentials([file(credentialsId: 'lukaszlenart-repository-access-token', variable: 'CUSTOM_SETTINGS')]) {
                  sh 'mvn -U -B -e clean install -Pdeploy,everything,nochecks -Dmaven.test.skip.exec=true -V -DobrRepository=NONE'
                }
              }
            }
            stage('Code Quality') {
              when {
                branch 'master'
              }
              steps {
                withCredentials([string(credentialsId: 'asf-cxf-sonarcloud', variable: 'SONARCLOUD_TOKEN')]) {
                  sh 'mvn sonar:sonar -DskipAssembly -Dsonar.projectKey=cxf -Dsonar.organization=apache -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=${SONARCLOUD_TOKEN}'
                }
              }
            } */
            }
            post {
              always {
                cleanWs deleteDirs: true, patterns: [[pattern: '**/target/**', type: 'INCLUDE']]
              }
            }
          }
        }
      }
    }
  }
}
