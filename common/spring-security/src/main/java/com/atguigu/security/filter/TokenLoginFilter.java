package com.atguigu.security.filter;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.jwt.JwtHelper;
import com.atguigu.common.result.ResponseUtil;
import com.atguigu.common.result.Result;
import com.atguigu.common.result.ResultCodeEnum;
import com.atguigu.security.custom.CustomUser;
import com.atguigu.vo.system.LoginVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
/**
 * <p>
 * 登录过滤器，继承UsernamePasswordAuthenticationFilter，对用户名密码进行登录校验
 * </p>
 */
//一登录接口（filter过滤器）：判断用户名和密码是否正确，生成token

public class TokenLoginFilter extends UsernamePasswordAuthenticationFilter {
    
    private RedisTemplate redisTemplate;
    //1.构造方法
    public TokenLoginFilter(AuthenticationManager authenticationManager,
                            RedisTemplate redisTemplate){
        this.setAuthenticationManager(authenticationManager);
        this.setPostOnly(false);
        //指定登录接口及提交方式，可以指定任意路径
        this.setRequiresAuthenticationRequestMatcher(
                new AntPathRequestMatcher("/admin/system/index/login","POST"));
        this.redisTemplate = redisTemplate;
    }

    //2.登录认证  重写父类方法 得到输入的用户名和密码
    //获取输入的用户名和密码， 封装成框架要求的对象  调用方法认证authenticate
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException{
        try {
            //获取输入的用户信息
            LoginVo loginVo = new ObjectMapper().readValue(request.getInputStream(), LoginVo.class);
            //封装对象
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(loginVo.getUsername(), loginVo.getPassword());
            //调用方法
            return this.getAuthenticationManager().authenticate(authentication);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //3.认证成功 调用方法 生成token
    @Override
    protected void successfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        //获取当前用户
        CustomUser customUser = (CustomUser) authResult.getPrincipal();
        //生成token
        String token = JwtHelper.createToken(customUser.getSysUser().getId(), customUser.getSysUser().getUsername());

        //获取当前用户的权限数据。放到redis里面可以：username value：权限数据
        redisTemplate.opsForValue().set(customUser.getUsername(), JSON.toJSONString(customUser.getAuthorities()));
        //返回
        Map<String, Object> map = new HashMap<>();
        map.put("token",token);
//        System.out.println("map = " + map);
        ResponseUtil.out(response, Result.ok(map));

    }

        //4.认证失败 调用方法
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
            ResponseUtil.out(response, Result.build(null, ResultCodeEnum.LOGIN_ERROR));
    }
}
