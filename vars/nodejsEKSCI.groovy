def call(Map configMap){
    def component = configMap.get('component')
    echo "Component is : $component"

    pipeline{
        
        agent {
            node {
                label 'AGENT-1'
            }
        }

        // agent any

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
                    sh "zip -r ${component}.zip ./* --exclude .git --exclude .zip"
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
                        nexusUrl: '172.31.37.24:8081/',
                        groupId: 'com.roboshop',
                        version: "${packageVersion}",
                        repository: "${component}",
                        credentialsId: 'nexus-auth',
                        artifacts: [
                            [artifactId: "${component}",
                            classifier: '',
                            file: "${component}.zip",
                            type: 'zip']
                        ]
                    )
                }
            }

            stage('Docker Build') {

                steps{
                    script{
                        sh """
                            docker build -t dockerjackal/${component}:${packageVersion} .
                        """
                    }
                }
            }

            // just make sure you login inside agent
            stage('Docker Push') {

                steps{
                    script{
                        sh """
                            docker push dockerjackal/${component}:${packageVersion}
                        """
                    }
                }
            }

            stage('EKS Deploy') {

                steps{
                    script{
                        sh """
                            cd helm
                            sed -i 's/IMAGE_VERSION/$packageVersion/g' values.yaml
                            helm upgrade ${component} .
                        """
                    }
                }
            }
            // here i need to configure downstream job. I have to pass package version for deployment
            // This job will wait until downstream job is over
            // By default, when a non master branch CI is done, we can go for DEV deployment

        //     stage('Deployment') {
        //         steps{
        //             script{
        //                 echo 'Deploying to Server'
        //                 def params = [
        //                     string(name: 'version', value: "${packageVersion}"),
        //                     string(name: 'environment', value: 'dev')
        //                 ]
        //                 build job: "../${component}-deploy", wait: true, parameters: params
        //             }
        //         }
        //     }
        // }

        post{
            always{
                echo 'Cleaning Up Workspace'
                deleteDir()
            }
        }



    }

}

