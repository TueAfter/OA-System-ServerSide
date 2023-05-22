package com.atguigu.auth.activiti;

import org.springframework.stereotype.Component;

@Component
public class UserBean {
    public String getUsername(int id){
        if(id == 1){
            return "zsy1";
        }
        if(id == 2){
            return "zsy2";
        }else {
            return "admin";
        }
    }
}
