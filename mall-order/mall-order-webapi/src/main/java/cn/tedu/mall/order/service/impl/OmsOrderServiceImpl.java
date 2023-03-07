package cn.tedu.mall.order.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.mapper.OmsOrderItemMapper;
import cn.tedu.mall.order.mapper.OmsOrderMapper;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.order.utils.IdGeneratorUtils;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import cn.tedu.mall.pojo.order.dto.CartUpdateDTO;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.product.service.order.IForOrderSkuService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @Description:
 * @Author: sweeterjava@163.com
 * @Date: 2023/3/7
 * @Time: 14:02
 */

//订单管理模块的业务逻辑层实现类,因为后面秒杀模块需要生成订单的功能,所以注册到dubbo
@DubboService
@Service
@Slf4j
public class OmsOrderServiceImpl implements IOmsCartService {

    // dubbo调用减少库存数的方法
    @DubboReference
    private IForOrderSkuService dubboSkuService;
    @Autowired
    private IOmsCartService omsCartService;
    @Autowired
    private OmsOrderMapper omsOrderMapper;
    @Autowired
    private OmsOrderItemMapper omsOrderItemMapper;

    // 新增订单的方法
    // 这个方法dubbo调用了product模块的方法,操作了数据库,有分布式事务的需求
    // 需要使用注解激活Seata分布式事务的功能
    @GlobalTransactional
    @Override
    public OrderAddVO addOrder(OrderAddDTO orderAddDTO) {
        OmsOrder order = new OmsOrder();
        BeanUtils.copyProperties(orderAddDTO, order);
        loadOrder(order);

        return null;
    }

    // 给order对象补全属性值的方法
    private void loadOrder(OmsOrder order) {
        Long id = IdGeneratorUtils.getDistributeId("order");
        order.setId(id);

        // 生成uuid
        order.setSn(UUID.randomUUID().toString());

        // 赋值userId
        if (order.getUserId() == null) {
            // 从SpringSecurity上下文中获取当前登录用户id
            order.setUserId(getUserId());
        }

        // 判断订单状态，如果为null，设置默认值为0
        if (order.getState() == null) {
            order.setState(0);
        }

        // 为了保证下单时间gmt_order和数据创建gmt_create时间一致
        // 我们在代码中为它们赋相同的值
        LocalDateTime now = LocalDateTime.now();
        order.setGmtOrder(now);
        order.setGmtCreate(now);
        order.setGmtModified(now);

        // 后端代码对实际应付金额进行验算,以求和前端数据一致
        // 实际应付金额=原价-优惠+运费
        // 金钱相关数据使用BigDecimal类型,防止浮点偏移的误差,取消取值范围限制


    }

    public OmsOrderServiceImpl() {
    }


    @Override
    public void addCart(CartAddDTO cartDTO) {

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

    public CsmallAuthenticationInfo getUserInfo() {
        // 编写获取SpringSecurity上下文的代码
        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken)
                        SecurityContextHolder.getContext().getAuthentication();
        // 为了逻辑严谨,判断一下SpringSecurity上下文中信息是不是null
        if (authenticationToken == null) {
            throw new CoolSharkServiceException(
                    ResponseCode.UNAUTHORIZED, "您没有登录!");
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
