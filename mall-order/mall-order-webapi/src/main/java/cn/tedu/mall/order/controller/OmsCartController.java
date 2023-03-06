package cn.tedu.mall.order.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description:
 * @Author: sweeterjava@163.com
 * @Date: 2023/3/3
 * @Time: 15:34
 */

@RestController
@RequestMapping("/oms/cart")
@Api(tags = "购物车管理模块")
public class OmsCartController {

    @Autowired
    private IOmsCartService omsCartService;

    @PostMapping("/add")
    @ApiOperation("新增sku信息到购物车")

    // 在程序运行控制器方法前,已经运行了过滤器代码,解析了JWT
    // 解析正确的话,已经将用户信息保存在了SpringSecurity上下文中
    // 酷鲨商城项目前台用户登录时,登录代码中会给用户一个固定的权限名ROLE_user
    // 下面的注解,主要目的是判断用户是否登录,权限统一设置为ROLE_user
    // 判断SpringSecurity上下文中是否存在这个权限,如果没有登录返回401错误
    @PreAuthorize("hasAuthority('ROLE_user')")

    // @Validated注解是激活SpringValidation框架用的
    // 参数CartAddDTO中,各个属性设置了验证规则,如果有参数值不符合规则
    // 会抛出BindException异常,由统一异常处理类处理,控制方法就不会运行了
    public JsonResult addCart(@Validated CartAddDTO cartAddDTO) {
        omsCartService.addCart(cartAddDTO);
        return JsonResult.ok("新增sku到购物车完成!");

    }


    @GetMapping("/list")
    @ApiOperation("根据用户Id分页查询购物车sku列表")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "页码", name = "page", example = "1"),
            @ApiImplicitParam(value = "每页条数", name = "pageSize", example = "10")
    })
    @PreAuthorize("hasAnyAuthority('ROLE_user')")
    public JsonResult<JsonPage<CartStandardVO>> listCartByPage(Integer page, Integer pageSize) {
        JsonPage<CartStandardVO> jsonPage = omsCartService.listCarts(page, pageSize);
        return JsonResult.ok(jsonPage);
    }

    @PostMapping("/delete")
    @ApiOperation("根据id数组删除购物车sku信息")
    @ApiImplicitParam(value = "要删除的id数组", name = "ids", required = true, dataType = "arrray")
    @PreAuthorize("hasAnyAuthority('ROLE_user')")
    public JsonResult removeCartByIds(Long[] ids){
        omsCartService.removeCart(ids);
        return JsonResult.ok("删除完成！");
    }
}
