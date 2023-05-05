package com.atguigu.auth;

import com.atguigu.auth.mapper.SysRoleMapper;
import com.atguigu.model.system.SysRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class TestMpDemo1 {

    @Autowired
    private SysRoleMapper mapper;

    //查询所有记录
    @Test
    public void getAll(){
        List<SysRole> sysRoles = mapper.selectList(null);
        System.out.println("sysRoles = " + sysRoles);
    }

    @Test
    public void add(){
        SysRole sysRole = new SysRole();
        sysRole.setRoleName("ddd");

    }

//    删除操作
    @Test
    public void deleteId(){
        mapper.deleteById(10);
}

//批量删除
    @Test
    public void deletep(){
        int result = mapper.deleteBatchIds(Arrays.asList(1, 1));
    }

    //条件查询
    @Test
    public void testQuery01(){
//        创建QueryWrapper对象
        QueryWrapper<SysRole> queryWrapper = new QueryWrapper<SysRole>();
        queryWrapper.eq("role_name","总经理");
        List<SysRole> list = mapper.selectList(queryWrapper);
    }

    @Test
    public void testQuery02(){
//        创建QueryWrapper对象
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<SysRole>();
        queryWrapper.eq(SysRole::getRoleName,"总经理");
        List<SysRole> list = mapper.selectList(queryWrapper);
    }

}
