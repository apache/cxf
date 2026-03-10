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
            values 'jdk_17_latest', 'jdk_21_latest', 'jdk_25_latest'
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
                  sh 'mvn -U -B clean install -Peverything'
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
                branch 'main'
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
                branch 'main'
              }
              steps {
                withCredentials([file(credentialsId: 'lukaszlenart-repository-access-token', variable: 'CUSTOM_SETTINGS')]) {
                  sh 'mvn -U -B -e clean install -Pdeploy,everything,nochecks -Dmaven.test.skip.exec=true -V -DobrRepository=NONE'
                }
              }
            }
            stage('Code Quality') {
              when {
                branch 'main'
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
  post {
    // If this build failed, send an email to the list.
    failure {
      script {
        emailext(
            to: "notifications@cxf.apache.org",
            recipientProviders: [[$class: 'DevelopersRecipientProvider']],
            from: "Mr. Jenkins <jenkins@builds.apache.org>",
            subject: "Jenkins job ${env.JOB_NAME}#${env.BUILD_NUMBER} failed",
            body: """
There is a build failure in ${env.JOB_NAME}.

Build: ${env.BUILD_URL}
Logs: ${env.BUILD_URL}console
Changes: ${env.BUILD_URL}changes

--
Mr. Jenkins
Director of Continuous Integration
"""
        )
      }
    }

    // If this build didn't fail, but there were failing tests, send an email to the list.
    unstable {
      script {
        emailext(
            to: "notifications@cxf.apache.org",
            recipientProviders: [[$class: 'DevelopersRecipientProvider']],
            from: "Mr. Jenkins <jenkins@builds.apache.org>",
            subject: "Jenkins job ${env.JOB_NAME}#${env.BUILD_NUMBER} unstable",
            body: """
Some tests have failed in ${env.JOB_NAME}.

Build: ${env.BUILD_URL}
Logs: ${env.BUILD_URL}console
Changes: ${env.BUILD_URL}changes

--
Mr. Jenkins
Director of Continuous Integration
"""
        )
      }
    }

    // Send an email, if the last build was not successful and this one is.
    fixed {
      script {
        emailext(
            to: "notifications@cxf.apache.org",
            recipientProviders: [[$class: 'DevelopersRecipientProvider']],
            from: 'Mr. Jenkins <jenkins@builds.apache.org>',
            subject: "Jenkins job ${env.JOB_NAME}#${env.BUILD_NUMBER} back to normal",
            body: """
The build for ${env.JOB_NAME} completed successfully and is back to normal.

Build: ${env.BUILD_URL}
Logs: ${env.BUILD_URL}console
Changes: ${env.BUILD_URL}changes

--
Mr. Jenkins
Director of Continuous Integration
"""
        )
      }
    }
  }
}
