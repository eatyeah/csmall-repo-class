package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsOrderItem;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Description:
 * @Author: sweeterjava@163.com
 * @Date: 2023/3/6
 * @Time: 12:13
 */

@Repository
public interface OmsOrderItemMapper {
    // 新增订单项(oms_order_item)的方法
    // 一个订单可能包含多件商品,每件商品都单独新增到数据库的话,会造成连库次数多,效率低
    // 我们这里采用一次连库新增多条数据的方式,来减少连库次数,提升操作效率
    // 所以我们参数就换成了集合类型List<OmsOrderItem>
    int insertOrderItemList(List<OmsOrderItem> omsOrderItems);
}
