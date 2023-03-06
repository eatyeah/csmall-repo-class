package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsOrder;
import org.springframework.stereotype.Repository;

/**
 * @Description:
 * @Author: sweeterjava@163.com
 * @Date: 2023/3/6
 * @Time: 17:12
 */

@Repository
public interface OmsOrderMapper {
    // 新增订单的方法
    int insertOrder(OmsOrder omsOrder);
}
