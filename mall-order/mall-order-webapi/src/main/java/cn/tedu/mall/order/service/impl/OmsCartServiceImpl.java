package cn.tedu.mall.order.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.mapper.OmsCartMapper;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import cn.tedu.mall.pojo.order.dto.CartUpdateDTO;
import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * @Description:
 * @Author: sweeterjava@163.com
 * @Date: 2023/3/3
 * @Time: 15:03
 */

@Service
@Slf4j
public class OmsCartServiceImpl implements IOmsCartService {

    @Autowired
    private OmsCartMapper omsCartMapper;

    // 新增sku信息到购物车
    @Override
    public void addCart(CartAddDTO cartDTO) {
        // 要查询当前登录用户的购物车中是否已经包含指定商品,需要先获得当前用户id
        // 利用封装好的方法直接从SpringSecurity上下文中获取
        Long userId = getUserId();
        // 根据用户Id和商品skuId,查询商品信息
        OmsCart omsCart = omsCartMapper.selectExistsCart(userId, cartDTO.getSkuId());
        // 判断查询出的omsCart是否为null
        if (omsCart == null) {
            // omsCart为null,表示当前用户购物车中没有这个sku商品
            // 所以要执行新增操作,新增操作需要一个OmsCart对象
            OmsCart newCart = new OmsCart();
            // 将参数cartDTO中和OmsCart中同名的属性赋值到newCart对象
            BeanUtils.copyProperties(cartDTO, newCart);
            // cartDTO中没有userId属性,需要单独赋值
            newCart.setUserId(userId);
            omsCartMapper.saveCart(newCart);
            omsCartMapper.saveCart(newCart);
        } else {
            // 如果omsCart不是null,表示当前用户购物车中已经有这个商品了
            // 我们需要做的就是将购物车中原有的数量和新增的数量相加,保存到数据库中
            // 购物车中原有的数量是omsCart.getQuantity(),新增的数量是cartDTO.getQuantity()
            // 所以我们可以将这两个数量相加的和赋值给omsCart属性
            omsCart.setQuantity(omsCart.getQuantity() + cartDTO.getQuantity());
            // 确定了数量之后,调用我们的持久层方法进行修改
            omsCartMapper.updateQuantityById(omsCart);

        }

    }

    @Override
    public JsonPage<CartStandardVO> listCarts(Integer page, Integer pageSize) {
        return null;
    }

    @Override
    public void removeCart(Long[] ids) {

    }

    @Override
    public void removeAllCarts() {

    }

    @Override
    public void removeUserCarts(OmsCart omsCart) {

    }

    @Override
    public void updateQuantity(CartUpdateDTO cartUpdateDTO) {

    }

    // 业务逻辑层中有获得当前登录用户信息的需求
    // 我们的项目会在控制器方法运行前运行的过滤器代码中,解析前端传入的JWT
    // 在过滤器中,将JWT解析的结果(用户信息)保存到SpringSecurity上下文
    // 所以里可以编写代码从SpringSecurity上下文中获得用户信息
    public CsmallAuthenticationInfo getUserInfo() {
        // 编写获取SpringSecurity上下文的代码
        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken)
                        SecurityContextHolder.getContext().getAuthentication();
        // 为了逻辑严谨,判断一下SpringSecurity上下文中信息是不是null
        if (authenticationToken == null) {
            throw new CoolSharkServiceException(ResponseCode.UNAUTHORIZED, "您没有登录");
        }
        // 从SpringSecurity上下文中获得用户信息
        CsmallAuthenticationInfo csmallAuthenticationInfo =
                (CsmallAuthenticationInfo) authenticationToken.getCredentials();
        // 最终别忘了返回
        return csmallAuthenticationInfo;

    }

    // 业务逻辑层需求中,实际上只需要用户的id
    // 我们可以再编写一个方法,从用户对象中获取id
    public Long getUserId() {
        return getUserInfo().getId();
    }

}









