package com.atguigu.auth.service.impl;

import com.atguigu.auth.service.SysMenuService;
import com.atguigu.auth.service.SysUserService;
import com.atguigu.model.system.SysUser;
import com.atguigu.security.custom.CustomUser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
//    UserDetailsService  可以用自己的接口  也可以在接口里继承security中类
    @Autowired
    SysUserService sysUserService;

    @Autowired
    SysMenuService sysMenuService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserService.getUserByUsername(username);
        if(null == sysUser) {
            throw new UsernameNotFoundException("用户名不存在！");
        }
        if(sysUser.getStatus().intValue() == 0) {
            throw new RuntimeException("账号已停用");
        }
        //根据用户id查询用户操作权限的数据
        List<String> userPermsList = sysMenuService.findUserPermsByUserId(sysUser.getId());
        //创建list集合，封装最终权限数据
        List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
        //查询list集合遍历
        for (String perms : userPermsList) {
            authorityList.add(new SimpleGrantedAuthority(perms.trim()));
        }
//        System.out.println("authorityList = " + authorityList);

        return new CustomUser(sysUser, authorityList);
    }
}

