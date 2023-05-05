package com.atguigu.auth.service.impl;

import com.atguigu.auth.mapper.SysRoleMapper;
import com.atguigu.auth.service.SysRoleService;
import com.atguigu.auth.service.SysUserRoleService;
import com.atguigu.model.system.SysRole;
import com.atguigu.model.system.SysUserRole;
import com.atguigu.vo.system.AssginRoleVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
    //    @Autowired
//    private SysRoleMapper sysRoleMapper;
//    =extends ServiceImpl<SysRoleMapper, SysRole>

    @Autowired
    private SysUserRoleService service;
    //查询所有的roleid
    @Override
    public Map<String, Object> findRoleDataByUserId(Long userId) {
        //1查询所有的角色，返回list集合 返回
        List<SysRole> allRolesList = baseMapper.selectList(null);
        //2根据用户id查询 角色用户关系表，查询userid对应的所有角色id
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId,userId);
        List<SysUserRole> existUserRoleList = service.list(wrapper);
        //从查询出来的用户id对应的角色的list集合，获取所有的角色id
//        List<Long> list1 = new ArrayList<>();
//        for (SysUserRole sysUserRole : existUserRoleList) {
//            Long roleId = sysUserRole.getRoleId();
//            list1.add(roleId);
//        }
//        上等于==下
        List<Long> existRoleIdList =
                existUserRoleList.stream().map(c -> c.getRoleId()).collect(Collectors.toList());

        //3根据查询出的所有角色id 找到对应的信息
        //根据角色id到所有的角色的list集合进行比较
        List<SysRole> assignRoleList = new ArrayList<>();
        for (SysRole sysRole : allRolesList) {
            if(existRoleIdList.contains(sysRole.getId())){
                assignRoleList.add(sysRole);
            }
        }
        //4把达到的两部分数据封装到map集合中返回
        Map<String, Object> roleMap = new HashMap<>();
        roleMap.put("assginRoleList", assignRoleList);
        roleMap.put("allRolesList", allRolesList);
        return roleMap;
    }

    //为用户分配角色  wapper为条件
    @Override
    public void doaSSign(AssginRoleVo assginRoleVo) {
        //把用户之前分配角色数据删除，用户角色关系表 根据userid删除
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper();
        wrapper.eq(SysUserRole::getUserId,assginRoleVo.getUserId());
        service.remove(wrapper);

        //重新进行分配
        List<Long> roleIdList = assginRoleVo.getRoleIdList();
        for (Long roleId : roleIdList) {
            if(StringUtils.isEmpty(roleId)){
                continue;
            }
            SysUserRole sysUserRole = new SysUserRole();
            sysUserRole.setUserId(assginRoleVo.getUserId());
            sysUserRole.setRoleId(roleId);

            service.save(sysUserRole);

        }
    }


}
