#!groovy

def decidePipeline(Map configMap){
    echo "I need to take decision based on the map you sent"
    application = configMap.get("application")
    // here we are getting nodeJs_VM_CI

    switch(application) {
        case 'nodejsVM':
            echo 'Application is NodeJS and VM based'
            nodejsVMCI(configMap)
            break
        case 'nodejsEKS':
            echo 'Application is NodeJS and Microservice based'
            nodejsEKSCI(configMap)
            break
        case 'javaVM':
            echo 'Application is Java and VM based'
            javaVMCI(configMap)
            break
        default:
            error "Un Recognised application"
            break
    }
}