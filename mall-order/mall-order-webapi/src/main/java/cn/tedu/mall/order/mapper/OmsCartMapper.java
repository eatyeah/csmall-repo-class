package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsCart;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

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
}
