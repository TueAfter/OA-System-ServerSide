package com.atguigu.auth.service.impl;


import com.atguigu.auth.mapper.SysMenuMapper;
import com.atguigu.auth.service.SysMenuService;
import com.atguigu.auth.service.SysRoleMenuService;
import com.atguigu.auth.util.MenuHelper;
import com.atguigu.common.exception.ZsyException;
import com.atguigu.model.system.SysMenu;
import com.atguigu.model.system.SysRoleMenu;
import com.atguigu.vo.system.AssginMenuVo;
import com.atguigu.vo.system.MetaVo;
import com.atguigu.vo.system.RouterVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单表 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-04-25
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Autowired
    private SysRoleMenuService sysRoleMenuService;
//    菜单列表接口
    @Override
    public List<SysMenu> findNodes() {
//        查询所有菜单数据
        List<SysMenu> sysMenuList = baseMapper.selectList(null);

//        2、构建成树形结构
//         {
//             第一层
//          children:[{
//             第二层
//        }]
//         }
        List<SysMenu> resultList = MenuHelper.buildTree(sysMenuList);


        return resultList;
    }

    //删除菜单
    @Override
    public void removeMenuById(Long id) {
        //判断当前菜单有没有下层菜单
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId,id);
        Integer count = baseMapper.selectCount(wrapper);
        if(count > 0){
            throw new ZsyException(201,"菜单不能删除");
        }
        baseMapper.deleteById(id);
    }

    @Override
    public List<SysMenu> findMenuByRoleId(Long roleId) {
        //查询所有的菜单--添加条件 status=1
        LambdaQueryWrapper<SysMenu> wrapperSysMenu = new LambdaQueryWrapper<>();
        wrapperSysMenu.eq(SysMenu::getStatus,1);
        List<SysMenu> allSysMenuList = baseMapper.selectList(wrapperSysMenu);

        //根据角色id 查询 角色菜单关系表里面 角色id对应的所有菜单id
        LambdaQueryWrapper<SysRoleMenu> wrapperSysRoleMenu = new LambdaQueryWrapper<>();
        wrapperSysRoleMenu.eq(SysRoleMenu::getRoleId,roleId);
        List<SysRoleMenu> sysRoleMenuList = sysRoleMenuService.list(wrapperSysRoleMenu);

        //根据获取的菜单id 获取对应的菜单对象
        List<Long> menuIdList = sysRoleMenuList.stream().map(c -> c.getMenuId()).collect(Collectors.toList());

        //拿着菜单id跟里面所有菜单集合id进行比较，如果相同封装
        allSysMenuList.stream().forEach(item -> {
            if(menuIdList.contains(item.getId())){
                item.setSelect(true);
            }else {
                item.setSelect(false);
            }
        });
        //返回规定树形显示菜单格式
        List<SysMenu> sysMenuList = MenuHelper.buildTree(allSysMenuList);


        return sysMenuList;
    }

    //为角色分配菜单
    @Override
    public void doAssign(AssginMenuVo assginMenuVo) {
        //根据角色id删除菜单表里面分配的数据
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, assginMenuVo.getRoleId());
        sysRoleMenuService.remove(wrapper);
        //从参数里面获取角色新分配的 菜单列表
        List<Long> menuIdList = assginMenuVo.getMenuIdList();
        for (Long menuId : menuIdList) {
            if (StringUtils.isEmpty(menuId)) {
                continue;
            }
            SysRoleMenu sysRoleMenu = new SysRoleMenu();
            sysRoleMenu.setMenuId(menuId);
            sysRoleMenu.setRoleId(assginMenuVo.getRoleId());
            sysRoleMenuService.save(sysRoleMenu);
            // 进行遍历 把每个id数据添加菜单角色表
        }
    }

    //4.根据用户id获取用户可以操作菜单列表
    @Override
    public List<RouterVo> findUserMenuListByUserId(Long userId) {
        List<SysMenu> sysMenuList = null;
        //1.判断当前用户是否是管理员 userId=1
        //1.1如果是管理员，根据userId查询所有菜单列表
        if(userId.longValue() == 1){
            LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysMenu::getStatus,1);
            wrapper.orderByAsc(SysMenu::getSortValue);  //排序  Asc升序
            sysMenuList = baseMapper.selectList(wrapper);

        }else {
            //1.2如果不是管理员，根据userId查询可以操作菜单列表
            //多表关联查询：用户角色  角色菜单 菜单
            sysMenuList = baseMapper.findMenuListByUserId(userId);
        }

        //2.把查询数据构建成框架要求的路由数据结构
        //使用菜单操作的工具类构建树形结构
        List<SysMenu> sysMenuTreeList = MenuHelper.buildTree(sysMenuList);
        //构建框架要求的路由数据结构
        List<RouterVo> routerList = this.buildRouter(sysMenuTreeList);
        return routerList;
    }

    //构建框架要求的路由数据结构
    private List<RouterVo> buildRouter(List<SysMenu> menus) {
        //创建list集合存储最终数据
        List<RouterVo> routers = new ArrayList<>();
        //遍历menus
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            router.setHidden(false);
            router.setAlwaysShow(false);
            router.setPath(getRouterPath(menu));
            router.setComponent(menu.getComponent());
            router.setMeta(new MetaVo(menu.getName(), menu.getIcon()));
            //封装下一次数据部分
            List<SysMenu> children = menu.getChildren();
            if(menu.getType().intValue() == 1){
                //加载下面隐藏路由
                List<SysMenu> hiddenMenuList = children.stream().filter(item -> !StringUtils.isEmpty(item.getComponent()))
                        .collect(Collectors.toList());
                for (SysMenu hiddenMenu : hiddenMenuList) {
                    RouterVo hiddenRouter = new RouterVo();
                    hiddenRouter.setHidden(true);
                    hiddenRouter.setAlwaysShow(false);
                    hiddenRouter.setPath(getRouterPath(hiddenMenu));
                    hiddenRouter.setComponent(hiddenMenu.getComponent());
                    hiddenRouter.setMeta(new MetaVo(hiddenMenu.getName(), hiddenMenu.getIcon()));
                    routers.add(hiddenRouter);
                }

            }else {
                if(!CollectionUtils.isEmpty(children)){
                    if(children.size() > 0) {
                        router.setAlwaysShow(true);
                    }
                    //递归
                    router.setChildren(buildRouter(children));
                }
            }

            routers.add(router);
        }
        return routers;
    }


    /**
     * 获取路由地址
     *
     * @param menu 菜单信息
     * @return 路由地址
     */
    public String getRouterPath(SysMenu menu) {
        String routerPath = "/" + menu.getPath();
        if(menu.getParentId().intValue() != 0) {
            routerPath = menu.getPath();
        }
        return routerPath;
    }

    //5.根据用户id获取用户可以操作的按钮列表
    @Override
    public List<String> findUserPermsByUserId(Long userId) {
        //1.判断当前用户是否是管理员 userId=1
        //1.1如果是管理员，根据userId查询所有按钮列表
        List<SysMenu> sysMenuList = null;
        if(userId.longValue() == 1){
            LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysMenu::getStatus,1);
            sysMenuList = baseMapper.selectList(wrapper);
        }else {
            //1.2如果不是管理员，根据userId查询可以操作菜单列表
            //多表关联查询：用户角色  角色菜单 菜单
            sysMenuList = baseMapper.findMenuListByUserId(userId);
        }

        //2.从查询出来的数据里面，获取可以操作按钮值list集合进行返回
        List<String> permsList = sysMenuList.stream()
                .filter(item -> item.getType() == 2)
                .map(item -> item.getPerms())
                .collect(Collectors.toList());

        return permsList;
    }
}
