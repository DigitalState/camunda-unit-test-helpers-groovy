package fluentapitest

import org.camunda.bpm.model.bpmn.Bpmn
import org.camunda.bpm.model.bpmn.BpmnModelInstance

trait BpmnFluentBuilder {

    BpmnModelInstance model1(){
        BpmnModelInstance model = Bpmn.createExecutableProcess('model')
        .startEvent()
        .scriptTask()
            .name('Some Simple Script')
            .scriptFormat('javascript')
            .camundaResource('deployment://fluentModelScript1.js')
        .userTask()
            .name('placeholder')
        .endEvent()
        .done()

        return model
    }

    BpmnModelInstance model2(){
        BpmnModelInstance model = Bpmn.createExecutableProcess('model')
                .name("Reminder Demo")
                .startEvent()
                .userTask('readEmail')
                    .boundaryEvent()
                        .timerWithDuration("PT1H")
                        .cancelActivity(false)
                        .manualTask()
                            .name('do something')
                        .endEvent()
                        .moveToActivity('readEmail')
                .boundaryEvent()
                    .timerWithCycle("R3/PT10M")
                    .manualTask()
                        .name('do something else')
                    .endEvent()
                    .moveToActivity('readEmail')
                .endEvent()
                .done()
        return model
    }

    BpmnModelInstance model3(){
        BpmnModelInstance model = Bpmn.createExecutableProcess('model')
                .name("Reminder Demo")
                .startEvent()
                .userTask('readEmail')
                    .boundaryEvent('killusertask')
                    .timerWithDuration("PT1H")
                    .cancelActivity(true)
                    .moveToActivity('readEmail')
                .boundaryEvent()
                    .timerWithCycle("R3/PT10M")
                    .cancelActivity(false)
                    .serviceTask()
                        .name('reminderSent')
                        .implementation('expression')
                        .camundaExpression('${1+1}')
                    .endEvent()
                    .moveToActivity('readEmail')
                .manualTask('manual1').name('do something')
                .moveToNode('killusertask').connectTo('manual1')
                //.moveToActivity('killusertask').connectTo('manual1') This does not work. Must use the moveToNode()
                .manualTask('manual2').name('do something else')
                .endEvent()
                .done()
        return model
    }


    BpmnModelInstance model4(){
        BpmnModelInstance model = Bpmn.createExecutableProcess('model')
                .startEvent()
                .subProcess()
                    .embeddedSubProcess()
                    .startEvent()
                    .manualTask()
                    .userTask("placeOrders")
                        .name("Place your order at: 1234")
                        .camundaAssignee("someUser")
                        .boundaryEvent("killUserTask")
                            .timerWithDuration("PT1H")
                            .cancelActivity(true)
                            .moveToActivity("placeOrders")
                        .boundaryEvent()
                            .timerWithCycle("R3/PT10M")
                            .cancelActivity(false)
                            .manualTask("reminderIfNeeded")
                            .endEvent()
                            .moveToActivity("placeOrders")
                    .serviceTask("timesUpOrOrderComplete")
                        .name("timesUpOrOrderComplete")
                        .implementation("expression")
                        .camundaExpression("\${1 + 1}")
                    .moveToNode("killUserTask").connectTo("timesUpOrOrderComplete")
                    .endEvent()
                .subProcessDone()
                    .multiInstance()
                    .parallel()
                    .camundaCollection("#{gettingLunch}")
                    .camundaElementVariable("lunchGetter")
                    .multiInstanceDone()
                .endEvent()
                .done()
        return model
    }

}