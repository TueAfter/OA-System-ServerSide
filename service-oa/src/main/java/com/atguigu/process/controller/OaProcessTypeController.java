package com.atguigu.process.controller;


import com.atguigu.common.result.Result;
import com.atguigu.model.process.ProcessType;
import com.atguigu.process.service.OaProcessTypeService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 审批类型 前端控制器
 * </p>
 *
 * @author atguigu
 * @since 2023-05-13
 */
@Api(value = "审批类型",tags = "审批类型")
@RestController
@RequestMapping("/admin/process/processType")
public class OaProcessTypeController {

    @Autowired
    OaProcessTypeService processTypeService;

    //查询所有审批分类
    @GetMapping("findAll")
    public Result findAll(){
        List<ProcessType> list = processTypeService.list();
        return Result.ok(list);
    }



    @ApiOperation(value = "分页")
    @GetMapping("{page}/{limit}")
    public Result index(@PathVariable Long page,
                        @PathVariable Long limit){
        Page<ProcessType> pageParam = new Page<>(page,limit);
        Page<ProcessType> pagemodel = processTypeService.page(pageParam);
        return Result.ok(pagemodel);
    }

    @ApiOperation(value = "获取")
    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id) {
        ProcessType processType = processTypeService.getById(id);
        return Result.ok(processType);
    }


    @ApiOperation(value = "新增")
    @PostMapping("save")
    public Result save(@RequestBody ProcessType processType) {
        processTypeService.save(processType);
        return Result.ok();
    }

    @ApiOperation(value = "修改")
    @PutMapping("update")
    public Result updateById(@RequestBody ProcessType processType) {
        processTypeService.updateById(processType);
        return Result.ok();
    }

    @ApiOperation(value = "删除")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {
        processTypeService.removeById(id);
        return Result.ok();
    }
}

