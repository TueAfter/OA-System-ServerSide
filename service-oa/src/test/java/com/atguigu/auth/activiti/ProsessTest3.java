package com.atguigu.auth.activiti;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipInputStream;

@SpringBootTest
public class ProsessTest3 {

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
                .addClasspathResource("process/jiaban04.bpmn20.xml")
                //png没有找到  在pop中resource include添加一个png
                .addClasspathResource("process/jiaban04.png")
                .name("加班04申请流程")
                .deploy();

        System.out.println(deploy.getId());
        System.out.println(deploy.getName());
    }

    @Test
    public void deployBuZip() {
        InputStream inputStream = this
                .getClass()
                .getClassLoader()
                .getResourceAsStream("process/qingjia.zip");
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

        //部署
        Deployment deployment = repositoryService.createDeployment()
                .addZipInputStream(zipInputStream)
                .name(zipInputStream.toString())
                .deploy();

        System.out.println("deployment id = " + deployment.getId());
        System.out.println("deployment name = " + deployment.getName());
    }

    //启动流程实例
    @Test
    public void startProcessInstance01(){
        runtimeService.startProcessInstanceByKey("jiaban04");
    }

    //2.查询组任务
    @Test
    public void findGroupTaskList(){
        List<Task> tom01 = taskService.createTaskQuery()
                .taskCandidateUser("rose01")
                .list();
        for (Task task : tom01) {
            System.out.println("----------------------------");
            System.out.println("流程实例id：" + task.getProcessInstanceId());
            System.out.println("任务id：" + task.getId());
            System.out.println("任务负责人：" + task.getAssignee());
            System.out.println("任务名称：" + task.getName());
        }
    }

    //3.拾取组任务
    @Test
    public void claimTask(){
        Task task = taskService.createTaskQuery()
                .taskCandidateUser("tom01")
                .singleResult();
        if(task != null){
            taskService.claim(task.getId(),"tom01");
            System.out.println("任务拾取完成");
        }
    }

    //4.查询某人代办任务--tom01
    @Test
    public void findTaskList(){
        String aggign = "tom01";
        List<Task> list = taskService.createTaskQuery()
                .taskAssignee(aggign).list();
        for (Task task : list) {
            System.out.println("流程实例的id"+task.getProcessDefinitionId());
            System.out.println("任务id：" + task.getId());
            System.out.println("任务负责人：" + task.getAssignee());
            System.out.println("任务名称：" + task.getName());
        }
    }

    //5.办理个人任务
    @Test
    public void completeTask(){
        //查询负责人需要处理的任务，返回一条
        Task task = taskService.createTaskQuery()
                .taskAssignee("tom01")
                .singleResult();
        //完成任务  参数：任务id   设置下一个任务点
        taskService.complete(task.getId());
    }

    //6.归还组任务


}
