package cn.tedu.mall.order.service;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import cn.tedu.mall.pojo.order.dto.CartUpdateDTO;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;

/**
 * <p>
 * 购物车数据表 服务类
 * </p>
 *
 * @author tedu.cn
 * @since 2022-02-16
 */
public interface IOmsCartService{
    // 新增订单的方法
    // 这个方法dubbo调用了product模块的方法,操作了数据库,有分布式事务的需求
    // 需要使用注解激活Seata分布式事务的功能
    @GlobalTransactional
    OrderAddVO addOrder(OrderAddDTO orderAddDTO);

    /**
     * 新增购物车
     * @param cartDTO
     */
    void addCart(CartAddDTO cartDTO);

    /**
     * 查询我的购物车
     * @param page
     * @param pageSize
     * @return
     */
    JsonPage<CartStandardVO> listCarts(Integer page, Integer pageSize);

    /**
     * 批量删除购物车
     * @param ids
     */
    void removeCart(Long[] ids);

    /**
     * 清空购物车
     */
    void removeAllCarts();

    /**
     *TODO 可以和removeAllCarts合并
     * @param omsCart
     */
    void removeUserCarts(OmsCart omsCart);
    /**
     * 更新购物车商品数量
     * @param cartUpdateDTO
     */
    void updateQuantity(CartUpdateDTO cartUpdateDTO);


}
