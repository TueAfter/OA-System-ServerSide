package com.atguigu.auth.controller;

import com.atguigu.auth.service.SysMenuService;
import com.atguigu.auth.service.SysUserService;
import com.atguigu.common.exception.ZsyException;
import com.atguigu.common.jwt.JwtHelper;
import com.atguigu.common.result.Result;
import com.atguigu.common.utils.MD5;
import com.atguigu.model.system.SysUser;
import com.atguigu.vo.system.LoginVo;
import com.atguigu.vo.system.RouterVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "后台登录管理")
@RestController
@RequestMapping("/admin/system/index")
public class IndexController {

    @Autowired
    SysUserService sysUserService;

    @Autowired
    SysMenuService sysMenuService;


    //login
    @ApiOperation("登录")
    @PostMapping("login")
    public Result login(@RequestBody LoginVo loginVo) {
//        String username = loginVo.getUsername();
//        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(SysUser::getUsername, username);
//        SysUser sysUser1 = sysUserService.getOne(wrapper);
        SysUser sysUser1 = sysUserService.getByUsername(loginVo.getUsername());
        System.out.println(sysUser1);
//        判断用户名
        if(sysUser1 == null){
            throw new ZsyException(201,"用户不存在！");
        }
        //判断密码
        //数据库存储密码
        String password_db = sysUser1.getPassword();
        //获取输入密码
        String password_input =MD5.encrypt(loginVo.getPassword());
        if(!password_db.equals(password_input)){
            throw new ZsyException(201,"密码错误");
        }
        //判断用户是否被禁用
        if(sysUser1.getStatus().intValue() == 0){
            throw new ZsyException(201,"用户已被禁用");
        }

        //使用jwt根据用户id和用户名称生成token字符串
        String token = JwtHelper.createToken(sysUser1.getId(), sysUser1.getUsername());
        Map<String,Object> map = new HashMap<>();
        map.put("token",token);
        System.out.println(map);

        return Result.ok(map);
    }

//    info 请求头
    @GetMapping("info")
    public Result info(HttpServletRequest res){
        //1.从请求头获取token字符串信息
        String token = res.getHeader("token");
        System.out.println(token);
        //2.从token字符串获取用户id和用户名称
        Long userId = JwtHelper.getUserId(token);
        //3.根据yhid查询数据库，把用户信息获取
        SysUser sysUser = sysUserService.getById(userId);
        //4.根据用户id获取用户可以操作的菜单列表
        //查询数据库动态构建出路由的结构，进行最终的显示
        List<RouterVo> routerList = sysMenuService.findUserMenuListByUserId(userId);
        //5.根据用户id获取用户可以操作的按钮列表
        List<String> persList = sysMenuService.findUserPermsByUserId(userId);
        //6.返回相应的数据

        Map<String,Object> map = new HashMap<>();
        map.put("roles","[admin]");
        map.put("name",sysUser.getName());
        map.put("avatar","https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");

        map.put("routers", routerList);
        map.put("buttons", persList);
        return Result.ok(map);
    }

    //退出
    @PostMapping("logout")
    public Result logout(){
        return Result.ok();
    }
}
