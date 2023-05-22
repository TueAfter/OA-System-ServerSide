package com.atguigu.auth.activiti;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
//@RunWith(SpringRunner.class)
public class ProcessTest2 {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    //如果test使用import org.junit 就在上面使用@RunWith(SpringRunner.class)注解
    @Test
    public void deployProcess(){
        //单个流程部署
        Deployment deploy = repositoryService.createDeployment()
//                .addZipInputStream() 压缩包部署
                .addClasspathResource("process/jiaban.bpmn20.xml")
                //png没有找到  在pop中resource include添加一个png
                .addClasspathResource("process/jiaban.png")
                .name("加班申请流程")
                .deploy();

        System.out.println(deploy.getId());
        System.out.println(deploy.getName());
    }

    //启动流程实例
    @Test
    public void startProcess(){
        Map<String,Object> map = new HashMap<>();
        map.put("assignee1","zsy1");
//        map.put("assignee2","zsy2");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("qingjia",map);
        System.out.println("流程定义id = " + processInstance.getProcessDefinitionId());
        System.out.println("流程实例id = " + processInstance.getId());
    }

    //查询某人代办任务
    @Test
    public void findTaskList(){
        String aggign = "zhangsan";
        List<Task> list = taskService.createTaskQuery()
                .taskAssignee(aggign).list();
        for (Task task : list) {
            System.out.println("流程实例的id"+task.getProcessDefinitionId());
            System.out.println("任务id：" + task.getId());
            System.out.println("任务负责人：" + task.getAssignee());
            System.out.println("任务名称：" + task.getName());
        }
    }

    @Test
    public void deployProcess01(){
        Deployment deploy = repositoryService.createDeployment()
                .addClasspathResource("process/jiaban...")
                .deploy();
    }

    //启动流程实例
    @Test
    public void startProcessInstance01(){
        runtimeService.startProcessInstanceByKey("jiaban01");
    }




    //处理当前任务
    @Test
    public void completeTask(){
        //查询负责人需要处理的任务，返回一条
        Task task = taskService.createTaskQuery()
                .taskAssignee("zsy1")
                .singleResult();

        Map<String,Object> map = new HashMap<>();
        map.put("assognee2","zsy2");
        //完成任务  参数：任务id   设置下一个任务点
        taskService.complete(task.getId(),map);
    }

    //查询已处理的任务
    @Test
    public void findcompleteTaskList(){
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee("zsy2")
                .finished().list();
//        System.out.println(list);
        for (HistoricTaskInstance historicTaskInstance : list) {
            System.out.println("流程实例的id"+historicTaskInstance.getProcessDefinitionId());
            System.out.println("任务id：" + historicTaskInstance.getId());
            System.out.println("任务负责人：" + historicTaskInstance.getAssignee());
            System.out.println("任务名称：" + historicTaskInstance.getName());
        }
    }

    //创建实例流程 ，指定BusinessKey
    @Test
    public void startUpProcessAddBusunessKey(){
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("qingjia", "1001");
        System.out.println(processInstance.getBusinessKey());
        System.out.println(processInstance.getId());
    }

    //全部流程实例挂起
    @Test
    public void suspendProcessInstandAll(){
        //获取流程定义的对象
        ProcessDefinition qingjia = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("qingjia").singleResult();
        //调用流程定义对象的方法判断当前状态 ：挂起 or 激活
        boolean suspended = qingjia.isSuspended();
        //判断如果挂起，实现激活
        if(suspended){
            //第一个参数  流程定义的id
            // 第二个参数  布尔类型 是否激活
            //第三个参数 时间点
            repositoryService.activateProcessDefinitionById(qingjia.getId(),true,null);
            System.out.println(qingjia.getId()+"激活了");
        }
        //如果激活，实现挂起
        else {
            repositoryService.suspendProcessDefinitionById(qingjia.getId(),true,null);
            System.out.println(qingjia.getId()+"挂起了");
        }
    }

    //单个的流程实例挂起
    @Test
    public void suspendProcessInstance(){
        String processInstanceID = "213c770d-f45d-11ed-862d-28cdc4143a0f";
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceID)
                .singleResult();
        boolean suspended = processInstance.isSuspended();
        if(suspended){
            //激活
            runtimeService.activateProcessInstanceById(processInstanceID);
            System.out.println(processInstanceID+"激活了");
        } else {
          runtimeService.suspendProcessInstanceById(processInstanceID);
            System.out.println(processInstanceID+"挂起了");
        }
    }

}
