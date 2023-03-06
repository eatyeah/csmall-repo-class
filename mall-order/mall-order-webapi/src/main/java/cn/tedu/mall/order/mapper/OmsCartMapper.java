package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Description:
 * @Author: sweeterjava@163.com
 * @Date: 2023/3/3
 * @Time: 14:25
 */

@Repository
public interface OmsCartMapper {
    // 判断当前用户的购物车中是否存在指定商品
    OmsCart selectExistsCart(@Param("userId") Long userId,
                             @Param("skuId") Long skuId);

    // 新增sku信息到购物车
    int saveCart(OmsCart omsCart);

    // 修改购物车中的sku商品数量
    int updateQuantityById(OmsCart omsCart);

    // 根据用户id查询该用户购物车中的sku信息
    List<CartStandardVO> selectCartByUserId(Long userId);

    // 根据用户选中的一个或多个id,删除购物车中的商品(支持批量删除)
    int deleteCartsByIds(Long[] ids);

}
