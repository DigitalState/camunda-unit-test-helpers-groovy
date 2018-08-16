package fluentapitest

import io.digitalstate.camunda.unittest.UnitTestingHelpers
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.camunda.bpm.engine.test.ProcessEngineRule
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification
//brings in Camunda BPM Assertion + AssertJ core.api.Assertions
// http://joel-costigliola.github.io/assertj/core/api/index.html
// http://camunda.github.io/camunda-bpm-assert/apidocs/org/camunda/bpm/engine/test/assertions/ProcessEngineTests.html
// http://joel-costigliola.github.io/assertj/
import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.repositoryService
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService

class FluentApiBuilderFeatureSpec extends Specification implements BpmnFluentBuilder, UnitTestingHelpers {

    @ClassRule
    @Shared ProcessEngineRule processEngineRule = new ProcessEngineRule('camunda_config/camunda.cfg.xml')

    def setupSpec(){
        // setDeploymentFiles() detects if the values are Strings or BpmnModelInstances
        // and executes the proper parsing.
        // If the value is a String it expects that the String is a Path to the file.
        setDeploymentFiles([
            'model1.bpmn': model3(),
            'fluentModelScript1.js': '/bpmn/fluentModelScript1.js'
        ])

        // deployNow() is a new method that has been injected into the metaclass
        // when setupDeployment() was executed.  deployNow() does the same as deploy()
        // but it also adds the deploymentId value into the Unit Test Helpers
        // to save the developer a extra step.
        setupDeployment().deployNow()
    }

    def 'Testing Fluent API Builder'() {
        when: 'Creating a instance of model1 process definition'
            ProcessInstance processInstance = runtimeService().startProcessInstanceByKey('model')

        then: 'Process is Active'
            assertThat(processInstance).isActive()
    }

    def cleanupSpec() {
        // Several SharedData methods are provided to add data into a static space for reuse
        // This saves a step of having to build a @Shared variable in the Unit Test's Class
        String deploymentId = getSharedData('deploymentId')

        // These two methods give you choice about which deployment to save into the build/target directory.
        // fromCamundaDB will use the deployment files saved into the DB.  Generally you dont want this
        // when using with tools such as Coverage Generation because the BPMN will not be the
        // original, but rather a modified version.
        // The FromSource uses the original un-modified files provided in the setDeploymentFiles() method.
        exportDeploymentFromCamundaDB()
        exportDeploymentFromSource()

        repositoryService().deleteDeployment(deploymentId,
                true, // cascade
                true, // skipCustomListeners
                true) // skipIoMappings
        println "Deployment ID: '${deploymentId}' has been deleted"
    }
}

