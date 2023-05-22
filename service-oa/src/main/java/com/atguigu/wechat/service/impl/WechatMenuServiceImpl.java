package com.atguigu.wechat.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.model.wechat.Menu;
import com.atguigu.vo.wechat.MenuVo;
import com.atguigu.wechat.mapper.WechatMenuMapper;
import com.atguigu.wechat.service.WechatMenuService;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-05-19
 */
@Service
public class WechatMenuServiceImpl extends ServiceImpl<WechatMenuMapper, Menu> implements WechatMenuService {


    @Autowired
    private WxMpService wxMpService;

    @Override
    public void syncMenu() {
        List<MenuVo> menuVoList = this.findMenuInfo();
        //菜单
        JSONArray buttonList = new JSONArray();
        for (MenuVo oneMenuVo : menuVoList) {
            JSONObject one = new JSONObject();
            one.put("name", oneMenuVo.getName());
            if (CollectionUtils.isEmpty(oneMenuVo.getChildren())) {
                one.put("type", oneMenuVo.getType());
                one.put("url", "http://oa.atguigu.cn/#" + oneMenuVo.getUrl());
            } else {
                JSONArray subButton = new JSONArray();
                for (MenuVo twoMenuVo : oneMenuVo.getChildren()) {
                    JSONObject view = new JSONObject();
                    view.put("type", twoMenuVo.getType());
                    if (twoMenuVo.getType().equals("view")) {
                        view.put("name", twoMenuVo.getName());
                        //H5页面地址
                        view.put("url", "http://oa.atguigu.cn#" + twoMenuVo.getUrl());
                    } else {
                        view.put("name", twoMenuVo.getName());
                        view.put("key", twoMenuVo.getMeunKey());
                    }
                    subButton.add(view);
                }
                one.put("sub_button", subButton);
            }
            buttonList.add(one);
        }
        //菜单
        JSONObject button = new JSONObject();
        button.put("button", buttonList);
        try {
            wxMpService.getMenuService().menuCreate(button.toJSONString());
        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    @Override
    public void removeMenu() {
        wxMpService.getMenuService().menuDelete();
    }


    //获取全部菜单
    @Override
    public List<MenuVo> findMenuInfo() {
        //1.查询所有菜单list集合
        List<MenuVo> list = new ArrayList<>();
        List<Menu> menuList = baseMapper.selectList(null);
        //2.查询所有一级菜单parent——id = 0 返回一级菜单list集合
        List<Menu> oneMenuList = menuList.stream().filter(menu -> menu.getParentId().longValue() == 0)
                .collect(Collectors.toList());
        //3.一级菜单list遍历，得到每一个一级菜单
        for (Menu menu : oneMenuList) {
            //一级菜单menu转换MenuVo
            MenuVo menuVo = new MenuVo();
            if (menu!=null){
                BeanUtils.copyProperties(menu, menuVo);
            }else {
                continue;
            }
            //4.获取每个一级菜单里面所有二级菜单 id和parent——id比较
            List<Menu> twoMenuList = menuList.stream().filter(menu1 -> menu1.getParentId().longValue() == menu.getId())
                    .collect(Collectors.toList());
            //5.把一级菜单里面所有二级菜单获取到，封装一级菜单children集合里面
            //涉及到Menu和MenuVo转换
            List<MenuVo> children = new ArrayList<>();
            for (Menu twomenu : twoMenuList) {
                MenuVo twomenuVo = new MenuVo();
                if (menu!=null){
                    BeanUtils.copyProperties(twomenu, twomenuVo);
                }else {
                    continue;
                }
                children.add(twomenuVo);
            }
            menuVo.setChildren(children);
            list.add(menuVo);
        }

        return list;
    }





}
