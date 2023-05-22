package com.atguigu.process.service.impl;

import com.atguigu.model.process.ProcessTemplate;
import com.atguigu.model.process.ProcessType;
import com.atguigu.process.mapper.OaProcessTemplateMapper;
import com.atguigu.process.service.OaProcessService;
import com.atguigu.process.service.OaProcessTemplateService;
import com.atguigu.process.service.OaProcessTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p>
 * 审批模板 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-05-13
 */
@Service
public class OaProcessTemplateServiceImpl extends ServiceImpl<OaProcessTemplateMapper, ProcessTemplate> implements OaProcessTemplateService {

    @Autowired
    OaProcessTypeService oaProcessTypeService;

    @Autowired
    private OaProcessService oaProcessService;

    @Override
    public IPage<ProcessTemplate> selectPageProcessTemplete(Page<ProcessTemplate> pageParam) {
        IPage<ProcessTemplate> processTemplatePage = baseMapper.selectPage(pageParam,null);
        List<ProcessTemplate> processTemplateList = processTemplatePage.getRecords();

        for (ProcessTemplate processTemplate : processTemplateList) {
            Long processTypeId = processTemplate.getProcessTypeId();
            LambdaQueryWrapper<ProcessType> wrapper = new LambdaQueryWrapper();
            wrapper.eq(ProcessType::getId,processTypeId);

            ProcessType processType = oaProcessTypeService.getOne(wrapper);
            if(processType == null){
                continue;
            }
            processTemplate.setProcessTypeName(processType.getName());

        }
        return processTemplatePage;
    }

    //部署流程
    @Transactional
    @Override
    public void publish(Long id) {
        ProcessTemplate processTemplate = baseMapper.selectById(id);
        processTemplate.setStatus(1);
        baseMapper.updateById(processTemplate);
        System.out.println("processTemplate = " + processTemplate);

        //TODO后续完善
        if(!StringUtils.isEmpty(processTemplate.getProcessDefinitionPath())){
            oaProcessService.deployBuZip(processTemplate.getProcessDefinitionPath());
        }
    }

}
