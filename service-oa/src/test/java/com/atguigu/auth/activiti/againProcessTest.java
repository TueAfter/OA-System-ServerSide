package com.atguigu.auth.activiti;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class againProcessTest {
    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    TaskService taskService;

    @Autowired
    HistoryService historyService;

    //单个流程部署
    @Test
    public void deployProcess(){
        Deployment deploy = repositoryService.createDeployment()
                .addClasspathResource("")
                .addClasspathResource("")
                .deploy();
    }

    //启动流程实例
    public void startProcessInstance(){
        ProcessInstance qingjia = runtimeService.startProcessInstanceByKey("qingjia");
    }

    //查询某人代办任务
    public void findProcess(){
        List<Task> list = taskService.createTaskQuery()
                .taskAssignee("zhangsan").list();
        for (Task task : list) {
            task.getId();
            task.getName();
            task.getAssignee();
        }
    }

    //处理当前任务
    public void completeTask(){
        Task zhangsan = taskService.createTaskQuery()
                .taskAssignee("zhangsan")
                .singleResult();
        taskService.complete(zhangsan.getId());
    }

    //查询以处理的任务
    public void findHistoryTask(){
        List<HistoricTaskInstance> zhangsan = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee("zhangsan")
                .finished().list();
        for (HistoricTaskInstance taskInstance : zhangsan) {
            taskInstance.getId();
        }
    }

    //创建实例流程，指定Busnessy
    public void startUpProcessAddBusunessKey(){
        ProcessInstance qingjia = runtimeService.startProcessInstanceByKey("qingjia", "1001");
        qingjia.getBusinessKey();
    }




}
