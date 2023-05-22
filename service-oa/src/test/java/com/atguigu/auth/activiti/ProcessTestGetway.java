package com.atguigu.auth.activiti;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class ProcessTestGetway {
    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    //1.部署流程定义  启动
    @Test
    public void deployProcess(){
        Deployment deploy = repositoryService.createDeployment()
                //因图片绘制错误失误
                .addClasspathResource("process/jiaban002.bpmn20.xml")
                //png没有找到  在pop中resource include添加一个png
                .name("请假002申请流程")
                .deploy();

        Map<String,Object> map = new HashMap<>();
        map.put("day",2);

        runtimeService.startProcessInstanceByKey("qingjia002",map);

        System.out.println(deploy.getId());
        System.out.println(deploy.getName());
    }

    //2.查询已处理的任务
    @Test
    public void findcompleteTaskList(){
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee("zhao2")
                .finished().list();
//        System.out.println(list);
        for (HistoricTaskInstance historicTaskInstance : list) {
            System.out.println("流程实例的id"+historicTaskInstance.getProcessDefinitionId());
            System.out.println("任务id：" + historicTaskInstance.getId());
            System.out.println("任务负责人：" + historicTaskInstance.getAssignee());
            System.out.println("任务名称：" + historicTaskInstance.getName());
        }
    }

    //3.完成处理当前任务
    @Test
    public void completeTask(){
        //查询负责人需要处理的任务，返回一条
        Task task = taskService.createTaskQuery()
                .taskAssignee("zhao6")
                .singleResult();
        //完成任务  参数：任务id   设置下一个任务点
        taskService.complete(task.getId());
    }



}
