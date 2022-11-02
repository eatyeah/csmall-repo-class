package cn.tedu.mall.order.controller;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oms/cart")
@Api(tags = "购物车管理模块")
public class OmsCartController {

    @Autowired
    private IOmsCartService omsCartService;

    @PostMapping("/add")
    @ApiOperation("新增购物车信息")
    // 在程序运行控制方法前,已经运行了过滤器中解析JWT的代码,
    // 解析正确的话,用户信息已经保存在了SpringSecurity上下文中
    // 酷鲨商城前台用户登录时,我们编写的代码会向用户信息中固定设置一个ROLE_user的角色
    // 下面的注解,主要目的是判断用户是否登录,权限统一都是ROLE_user
    // 如果用户没有登录,是无法运行控制方法的!401错误
    @PreAuthorize("hasAuthority('ROLE_user')")
    // @Validated注解是激活SpringValidation框架用的
    // 参数CartAddDTO中,各个属性设置了验证规则,如果有参数值不符合规则
    // 会抛出BindException异常,之后会运行统一异常处理类中专门的方法,控制器方法终止
    public JsonResult addCart(@Validated CartAddDTO cartAddDTO){
        omsCartService.addCart(cartAddDTO);
        return JsonResult.ok("新增sku到购物车完成!");
    }


}
