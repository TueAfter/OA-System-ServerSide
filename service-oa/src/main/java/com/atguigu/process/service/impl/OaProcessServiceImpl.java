package com.atguigu.process.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.auth.service.SysUserService;
import com.atguigu.common.result.Result;
import com.atguigu.model.process.Process;
import com.atguigu.model.process.ProcessRecord;
import com.atguigu.model.process.ProcessTemplate;
import com.atguigu.model.process.ProcessType;
import com.atguigu.model.system.SysUser;
import com.atguigu.process.mapper.OaProcessMapper;
import com.atguigu.process.service.OaProcessRecordService;
import com.atguigu.process.service.OaProcessService;
import com.atguigu.process.service.OaProcessTemplateService;
import com.atguigu.security.custom.LoginUserInfoHelper;
import com.atguigu.vo.process.ApprovalVo;
import com.atguigu.vo.process.ProcessFormVo;
import com.atguigu.vo.process.ProcessQueryVo;
import com.atguigu.vo.process.ProcessVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.swagger.annotations.ApiOperation;
import org.activiti.bpmn.model.*;
import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.xml.bind.util.JAXBSource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * <p>
 * 审批类型 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-05-14
 */
@Service
public class OaProcessServiceImpl extends ServiceImpl<OaProcessMapper, Process> implements OaProcessService {

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private OaProcessTemplateService processTemplateService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private OaProcessRecordService processRecordService;

    @Autowired
    private HistoryService historyService;

    @Override
    public IPage<ProcessVo> selectPage(Page<ProcessVo> pageParam, ProcessQueryVo processQueryVo) {
        IPage<ProcessVo> pageModel = baseMapper.selectPage(pageParam, processQueryVo);
        System.out.println("pageModel = " + pageModel);
        return pageModel;
    }

    @Override
    public void deployBuZip(String deployPath) {
        InputStream inputStream = this
                .getClass()
                .getClassLoader()
                .getResourceAsStream(deployPath);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

        //部署
        Deployment deployment = repositoryService.createDeployment()
                .addZipInputStream(zipInputStream)
                .deploy();

        System.out.println("deployment id = " + deployment.getId());
        System.out.println("deployment name = " + deployment.getName());
    }

    //启动流程
    @Override
    public void startup(ProcessFormVo processFormVo) {
        //1.根据当前用户id获取用户信息
        SysUser sysUser = sysUserService.getById(LoginUserInfoHelper.getUserId());
        //2.根据审批模板id把模板信息查询
        ProcessTemplate processTemplate = processTemplateService.getById(processFormVo.getProcessTemplateId());
        //3.保存提交审批信息到业务表 oa_process
        Process process = new Process();
        //processFormVo复制到process对象里面
        BeanUtils.copyProperties(processFormVo,process);

        String workNo = System.currentTimeMillis() + "";
        process.setProcessCode(workNo);
        process.setUserId(LoginUserInfoHelper.getUserId());
        process.setFormValues(processFormVo.getFormValues());
        process.setTitle(sysUser.getName() + "发起" + processTemplate.getName() + "申请");
        process.setStatus(1);
        baseMapper.insert(process);
        //4.启动流程实例
        //4.1 流程定义的key
        String processDefinitionKey = processTemplate.getProcessDefinitionKey();
        //4.2 业务key  processId
        String businessKey = String.valueOf(process.getId());
        System.out.println("businessKey = " + businessKey);
        //4.3流程参数 form表单json数据 转换map集合
        String formValues = process.getFormValues();
        //formData
        JSONObject jsonObject = JSON.parseObject(formValues);
        JSONObject formData = jsonObject.getJSONObject("formData");
        //遍历formData得到内容，封装map集合
        Map<String,Object> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            map.put(entry.getKey(),entry.getValue());
        }
        Map<String,Object> variables = new HashMap<>();
        variables.put("data",map);
        System.out.println("variables = " + variables);

        ProcessDefinition qingjia = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .singleResult();
        //调用流程定义对象的方法判断当前状态 ：挂起 or 激活
        boolean suspended = qingjia.isSuspended();
        //判断如果挂起，实现激活
        if(suspended){
            repositoryService.activateProcessDefinitionById(qingjia.getId(),true,null);
            System.out.println(qingjia.getId()+"激活了");
        }

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, variables);
        System.out.println("processInstance = " + processInstance);
        //5.查询下一个审批人
        //审批人可能多个
        List<Task> taskList = this.getCurrrentTaskList(processInstance.getId());
        List<String> nameList = new ArrayList<>();
        for (Task task : taskList) {
            String assigneeName = task.getAssignee();
            //用登录名称得到真实姓名
            SysUser user = sysUserService.getUserByUsername(assigneeName);
            String name = user.getName();

            nameList.add(name);
            //TODO 6.推送消息
        }

        //7.业务流程进行关联 更新oa_process
        process.setProcessInstanceId(processInstance.getId());
        process.setDescription("等待"+ StringUtils.join(nameList.toArray(),",") +"审批");
        baseMapper.updateById(process);
        processRecordService.record(process.getId(),1,"发起申请");

    }


    //当前任务列表
    private List<Task> getCurrrentTaskList(String id) {
        List<Task> taskList = taskService.createTaskQuery().processInstanceId(id).list();
        return taskList;
    }


    //查询待处理任务列表
    @Override
    public IPage<ProcessVo> findPending(Page<Process> pageParam) {
        //1.封装条件查询，根据当前登录的用户名称
        TaskQuery query = taskService.createTaskQuery()
                .taskAssignee(LoginUserInfoHelper.getUsername())
                .orderByTaskCreateTime()
                .desc();
        System.out.println("query = " + query);
        //2.调用方法分页条件查询，返回list集合，待办任务集合
        //listPage（开始位置，每页显示记录数）
        int begin = (int) ((pageParam.getCurrent()-1)*pageParam.getSize());
        int size = (int) pageParam.getSize();
        List<Task> taskList = query.listPage(begin, size);

        long count = query.count();
        System.out.println("count = " + count);
        //3.封装返回list集合数据 到list<ProcessVo>里面
        //List<Task>  -转换-  List<ProcessVo>
        List<ProcessVo> processVoList = new ArrayList<>();
        for (Task task : taskList) {
            //从task任务获取流程实例id
            String processInstanceId = task.getProcessInstanceId();
            //根据流程实例的id获取实例对象
            ProcessInstance processInstance = runtimeService
                    .createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
                if (processInstance == null) {
                    continue;
                }
            //从流程实例对象获取业务key  ---就是processId
            String businessKey = processInstance.getBusinessKey();
            System.out.println("businessKey = " + businessKey);
            if (businessKey == null){
                continue;
            }
            //根据业务key获取process对象
            long processId = Long.parseLong(businessKey);
            Process process = baseMapper.selectById(processId);
            System.out.println("process = " + process);
            //process对象 复制到processVo对象
            ProcessVo processVo = new ProcessVo();
            if (process!=null){
                BeanUtils.copyProperties(process, processVo);
            }else {
                continue;
            }

            processVo.setTaskId(task.getId());

            processVoList.add(processVo);
            System.out.println("processVo = " + processVo);

        }
        //4.封装返回page对象
        IPage<ProcessVo> page = new Page<ProcessVo>(pageParam.getCurrent(),pageParam.getSize(),count);
        page.setRecords(processVoList);
        return page;
    }



    @Override
    public Map<String, Object> show(Long id) {
        //1.根据流程id获取流程信息Proces
        Process process = baseMapper.selectById(id);
        //2.根据流程id获取流程记录信息
        LambdaQueryWrapper<ProcessRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessRecord::getProcessId,id);
        List<ProcessRecord> processRecordList = processRecordService.list(wrapper);
        //3.根据模板id查询模板信息
        ProcessTemplate processTemplate = processTemplateService.getById(process.getProcessTemplateId());
        //4.判断当前用户是否可以进行审批
        //可以看到信息的人不一定能审批 ，不能重复审批
        boolean isApprove = false;
        List<Task> taskList = this.getCurrrentTaskList(process.getProcessInstanceId());
        for (Task task : taskList) {
            //判断任务审批人是否是当前用户
            if(task.getAssignee().equals(LoginUserInfoHelper.getUsername())){
                isApprove = true;
            }
        }
        //5.封装数据到map集合 返回
        Map<String,Object> map = new HashMap<>();
        map.put("process", process);
        map.put("processRecordList", processRecordList);
        map.put("processTemplate", processTemplate);
        map.put("isApprove", isApprove);

        return map;
    }


    //审批
    @Override
    public void approve(ApprovalVo approvalVo) {
        //1.从approvalVo获取任务id ，根据任务id获取流程变量
        String taskId = approvalVo.getTaskId();
        Map<String, Object> variables = taskService.getVariables(taskId);
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            System.out.println("entry.getKey() = " + entry.getKey());
            System.out.println("entry.getValue() = " + entry.getValue());
        }
        //2.判断审批状态值
        if(approvalVo.getStatus() == 1){
            //2.1状态值 = 1 审批通过
            Map<String, Object> variable = new HashMap<>();
            taskService.complete(taskId,variable);
        }else {
            //2.1状态值 = -1 结束或驳回流程
            this.endTask(taskId);
        }

        //3.记录审批相关过程信息
        String description = approvalVo.getStatus().intValue() ==1 ? "已通过":"驳回";
        processRecordService.record(approvalVo.getProcessId(),approvalVo.getStatus(),description);
        //4.查询下一个审批人，跟新流程process表记录
        Process process = baseMapper.selectById(approvalVo.getProcessId());
        //查询任务
        List<Task> taskList = this.getCurrrentTaskList(process.getProcessInstanceId());
        if(!CollectionUtils.isEmpty(taskList)){
            List<String> assignList = new ArrayList<>();
            for (Task task : taskList) {
                String assignee = task.getAssignee();
                SysUser sysUser = sysUserService.getUserByUsername(assignee);
                assignList.add(sysUser.getName());

                //TODO 公众号消息推送
            }
            //更新process流程信息
            process.setDescription("等待" + StringUtils.join(assignList.toArray(), ",") + "审批");
            process.setStatus(1);
        }else {
            if(approvalVo.getStatus().intValue() == 1){
                process.setDescription("审批通过");
                process.setStatus(2);
            } else {
                process.setDescription("审批完成（拒绝）");
                process.setStatus(-1);
            }
        }
        baseMapper.updateById(process);
    }


    private void endTask(String taskId) {
        //1.根据任务id获取任务对象 Task
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        //2.获取流程定义模型
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        //3.获取结束的流向节点
        List<EndEvent> endEventList = bpmnModel.getMainProcess().findFlowElementsOfType(EndEvent.class);
        if(CollectionUtils.isEmpty(endEventList)){
            return;
        }
        FlowNode endFlowNode = (FlowNode)endEventList.get(0);
        //4.获取当前的流向节点
        FlowNode currentFlowNode = (FlowNode)bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey());
        //  临时保存当前活动的原始方向
        List originalSequenceFlowList = new ArrayList<>();
        originalSequenceFlowList.addAll(currentFlowNode.getOutgoingFlows());
        //5.清理当前流动方向
        currentFlowNode.getOutgoingFlows().clear();
        //6.创建新的流向
        SequenceFlow sequenceFlow = new SequenceFlow();
        sequenceFlow.setId("sequenceFlow");
        sequenceFlow.setSourceFlowElement(currentFlowNode);
        sequenceFlow.setTargetFlowElement(endFlowNode);
        //7.当前节点指向新方向
        List sequenceFlowList = new ArrayList();
        sequenceFlowList.add(sequenceFlow);
        currentFlowNode.setOutgoingFlows(sequenceFlowList);

        //8.完成当前任务
        taskService.complete(taskId);

    }


    //已处理
    @Override
    public IPage<ProcessVo> findProcessed(Page<Process> pageParam) {
        //1.封装查询条件
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(LoginUserInfoHelper.getUsername())
                .finished()
                .orderByTaskCreateTime().desc();
        //2.调用方法条件分页查询，返回list集合
        //（开始位置，每页显示记录数）
        int begin = (int) ((pageParam.getCurrent()-1)*pageParam.getSize());
        int size = (int)pageParam.getSize();
        List<HistoricTaskInstance> list = query.listPage(begin, size);
        long totalcount = query.count();
        //3.遍历返回list集合，封装List<ProcessVo>
        List<ProcessVo> processVoList = new ArrayList<>();
        for (HistoricTaskInstance instance : list) {
            String processInstanceId = instance.getProcessInstanceId();
            //根据流程实例id获取process信息
            LambdaQueryWrapper<Process> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Process::getProcessInstanceId,processInstanceId);
            Process process = baseMapper.selectOne(wrapper);

            ProcessVo processVo = new ProcessVo();
            if (process!=null){
                BeanUtils.copyProperties(process, processVo);
            }else {
                continue;
            }
            processVoList.add(processVo);
        }
        //4.IPage封装分页查询所有数据，返回
        IPage<ProcessVo> page = new Page(pageParam.getCurrent(),pageParam.getSize(),count());
        page.setRecords(processVoList);
        return page;
    }

    //已发起
    @Override
    public Object findStarted(Page<ProcessVo> pageParam) {
        ProcessQueryVo processQueryVo = new ProcessQueryVo();
        processQueryVo.setUserId(LoginUserInfoHelper.getUserId());
        IPage<ProcessVo> pageModel = baseMapper.selectPage(pageParam, processQueryVo);
        return pageModel;
    }


}
