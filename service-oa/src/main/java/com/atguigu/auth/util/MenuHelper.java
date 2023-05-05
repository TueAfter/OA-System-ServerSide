package com.atguigu.auth.util;


import com.atguigu.model.system.SysMenu;

import java.util.ArrayList;
import java.util.List;

public class MenuHelper {
    //使用递归方法构建菜单
    public static List<SysMenu> buildTree(List<SysMenu> sysMenuList) {
        List<SysMenu> tree = new ArrayList<>();
//         把所有的菜单遍历
        for (SysMenu sysMenu : sysMenuList) {
            //递归入口进入
//            parentId=0是入口
            if(sysMenu.getParentId().longValue() == 0){
                tree.add(getChildren(sysMenu,sysMenuList));
            }
        }
        return tree;
    }

    public static SysMenu getChildren(SysMenu sysMenu, List<SysMenu> sysMenuList){
        sysMenu.setChildren(new ArrayList<SysMenu>());
        for (SysMenu it : sysMenuList) {
            if(sysMenu.getId().longValue() == it.getParentId().longValue()){
                if(sysMenu.getChildren()==null){
                    sysMenu.setChildren(new ArrayList<>());
                }
                sysMenu.getChildren().add(getChildren(it,sysMenuList));
            }
        }
        return sysMenu;
    }
}
