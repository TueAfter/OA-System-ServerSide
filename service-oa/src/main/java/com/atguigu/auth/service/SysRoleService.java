package com.atguigu.auth.service;

import com.atguigu.model.system.SysRole;
import com.atguigu.vo.system.AssginRoleVo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public interface SysRoleService extends IService<SysRole> {
    Map<String,Object> findRoleDataByUserId(Long userId);

    void doaSSign(AssginRoleVo assginRoleVo);
}
