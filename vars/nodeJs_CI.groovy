pipeline{
    
    agent {
        node {
            label 'AGENT-1'
        }
    }

    // agent any

    parameters {
        string(name: 'component', defaultValue: '', description: 'Which component ?')
    }

    environment{
        packageVersion = ''
    }
    
    stages{

        stage('Get Version') {

            steps{

                script{
                    def packageJson = readJSON(file: 'package.json')
                    packageVersion = packageJson.version
                    echo "version: ${packageVersion}"
                }
            }
        }

        stage('Install Dependencies') {

            steps{
                sh 'pwd'
                sh 'ls -ltr'
                sh 'npm install'
            }
        }

        stage('Run Unit Tests') {
            steps{
                echo 'Running Unit Tests'
            }
        }

        // sonar-scanner command expects sonar-project.properties should be available
        stage('Sonar Scan') {
            steps{
                echo "Sonar Scan Done"
            }
        }

        stage('Build') {
            steps{
                sh "ls -ltr"
                sh "zip -r ${params.component}.zip ./* --exclude .git --exclude .zip"
            }
        }

        // Static Application Security Testing

        stage('SAST') {
            steps{
                echo "Static Application Security Testing Done"
            }
        }

        //Install Pipeline Utility steps plugin and Nexus Artifact Uploader
        stage('Publish Artifacts') {

            steps{
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: '34.229.101.93:8081/',
                    groupId: 'com.roboshop',
                    version: "$packageVersion",
                    repository: "${params.component}",
                    credentialsId: 'nexus-auth',
                    artifacts: [
                        [artifactId: "${params.component}",
                        classifier: '',
                        file: "${params.component}.zip",
                        type: 'zip']
                    ]
                )
            }
        }
        // here i need to configure downstream job. I have to pass package version for deployment
        // This job will wait until downstream job is over
        stage('Deployment') {
            steps{
                script{
                     echo 'Deploying to Server'
                     def params = [
                        string(name: 'version', value: "${packageVersion}")
                     ]
                    build job: "../${params.component}-deploy", wait: true, parameters: params
                }
               
            }
        }
    }

    post{
        always{
            echo 'Cleaning Up Workspace'
            deleteDir()
        }
    }






}