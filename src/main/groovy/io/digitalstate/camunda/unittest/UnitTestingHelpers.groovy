package io.digitalstate.camunda.unittest

import org.camunda.bpm.engine.repository.Deployment
import org.camunda.bpm.engine.repository.DeploymentBuilder
import org.camunda.bpm.model.bpmn.Bpmn
import org.camunda.bpm.model.bpmn.BpmnModelInstance

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*

trait UnitTestingHelpers {

    private Map<String, Object> sharedData = [:]

    Map<String, Object> getSharedData(){
        return this.sharedData
    }

    Object getSharedData(String key){
        return this.sharedData.get(key)
    }

    void setSharedData(Map<String, Object> sharedData){
        this.sharedData = sharedData
    }

    void addSharedData(Map<String, Object> sharedData){
        this.sharedData.putAll(sharedData)
    }

    void addSharedData(String key, Object value){
        this.sharedData.put(key, value)
    }

    private  Map<String, String> deploymentFiles = [:]

    Map<String, String> getDeploymentFiles(){
        return deploymentFiles
    }

    /**
     *
     * @param deploymentFiles Map of files to deploy: supports BpmnModelInstances and
     * String based path to a file in the classpath
     */
    void setDeploymentFiles(Map<String, Object> deploymentFiles){
        Map<String, Object> modifedDeployment = deploymentFiles.collectEntries { key, value ->
            if (value instanceof BpmnModelInstance) {
                [key, modelInstanceAsString((BpmnModelInstance) value)]

            } else if (value instanceof String) {
                [key, resourceAsString((String) value)]

            } else {
                throw new Exception("Unknown Deployment file format: ${key}  :  ${value}")
            }
        }
        this.deploymentFiles.putAll(modifedDeployment)
    }

    DeploymentBuilder setupDeployment(String deploymentName = 'FluentModelBuilder', duplicateFiltering = false){
        // Adds a deployNow() command which reroutes the DeploymentBuilder object into the custom deploy()
        // The custom deploy() is used to add additional data actions
        DeploymentBuilder.getMetaClass().deployNow = { -> deployNow((DeploymentBuilder)getDelegate()) }

        DeploymentBuilder deployment = repositoryService().createDeployment()
        this.deploymentFiles.each { file ->
            deployment.addString(file.getKey(), file.getValue())
        }
        deployment.name('FluentModelBuilder')
                .enableDuplicateFiltering(false)

        return deployment
    }

    /**
     * Deploy and set the sharedData Map with a deploymentId string value.
     * Typically used as a shortcut to reduce having to manually add the deploymentId to the sharedData map.
     * @param deploymentBuilder a DeploymentBuilder object to execute a deploy() on.
     * @return a Deployment object
     */
    Deployment deployNow(DeploymentBuilder deploymentBuilder){
        Deployment deployment = deploymentBuilder.deploy()
        addSharedData('deploymentId', deployment.getId())
        return deployment
    }

    // helper method to shorten the .addInputStream params in createDeployment()
    private String resourceAsString(String path){
        return this.getClass().getResource(path.toString()).getText('UTF-8')
    }

    private String modelInstanceAsString(BpmnModelInstance modelInstance){
        String model = Bpmn.convertToString(modelInstance)
        return model
    }

    void exportDeploymentFromCamundaDB(String deploymentId = getSharedData('deploymentId')){
        if (!deploymentId){
            throw new Exception('Cannot Export Deployment Files from Camunda DB: deploymentId in SharedData is null or does not exist ')
        }
        Map<String, InputStream> files = [:]
        repositoryService().getDeploymentResources(deploymentId).each {
            InputStream fileInputStream = repositoryService().getResourceAsStreamById(deploymentId, it.getId())
            String fileName = it.getName()
            files.put(fileName , fileInputStream)
        }
        FileTreeBuilder treeBuilder = new FileTreeBuilder()
        files.each { file ->
            treeBuilder {
                target {
                    'camunda-deployment-files-from-db' {
                        "${file.getKey()}" file.getValue().getText('UTF-8')
                    }
                }
            }
        }
    }

    void exportDeploymentFromSource(Map<String, String> files = this.getDeploymentFiles()){
        FileTreeBuilder treeBuilder = new FileTreeBuilder()
        files.each { file ->
            treeBuilder {
                target {
                    'camunda-deployment-files-from-source' {
                        "${file.getKey()}" file.getValue()
                    }
                }
            }
        }
    }

}