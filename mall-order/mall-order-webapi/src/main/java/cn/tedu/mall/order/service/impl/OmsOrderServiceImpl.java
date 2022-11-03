package cn.tedu.mall.order.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.mapper.OmsOrderItemMapper;
import cn.tedu.mall.order.mapper.OmsOrderMapper;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.order.utils.IdGeneratorUtils;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderItemAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderListTimeDTO;
import cn.tedu.mall.pojo.order.dto.OrderStateUpdateDTO;
import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.model.OmsOrderItem;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.pojo.order.vo.OrderDetailVO;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import cn.tedu.mall.product.service.order.IForOrderSkuService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 后面的秒杀业务需要调用这个生成订单的方法,所以需要支持dubbo调用
@DubboService
@Service
@Slf4j
public class OmsOrderServiceImpl implements IOmsOrderService {

    @DubboReference
    private IForOrderSkuService dubboSkuService;
    @Autowired
    private IOmsCartService omsCartService;
    @Autowired
    private OmsOrderMapper omsOrderMapper;
    @Autowired
    private OmsOrderItemMapper omsOrderItemMapper;

    // 新增订单的方法
    // 这个方法dubbo调用了Product模块的方法,操作了数据库,有分布式的事务需求
    // 所以要使用注解激活Seata分布式事务的功能
    @GlobalTransactional
    @Override
    public OrderAddVO addOrder(OrderAddDTO orderAddDTO) {
        // 第一部分:收集信息,准备数据
        // 先实例化OmsOrder对象
        OmsOrder order=new OmsOrder();
        // 当前方法参数orderAddDTO有很多order需要的同名属性,直接赋值接口
        BeanUtils.copyProperties(orderAddDTO,order);
        // orderAddDTO中属性比OmsOrder要少,缺少的属性要我们自己赋值或生成
        // 可以编写一个专门的方法,来进行数据的收集
        loadOrder(order);
        // 到此为止,order的普通属性全部赋值完毕
        // 下面要将参数orderAddDTO中包含的订单项(orderItem集合)信息赋值
        // 首先取出这个集合,也就是当前订单中包含的所有商品的集合
        List<OrderItemAddDTO> itemAddDTOs=orderAddDTO.getOrderItems();
        if(itemAddDTOs==null || itemAddDTOs.isEmpty()){
            // 如果当前订单中没有商品,就无法继续生成订单了
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,
                    "订单中必须至少包含一件商品");
        }
        // 我们最终的目标是将当前订单中包含的订单项新增到数据库
        // 当前集合泛型是OrderItemAddDTO,我们编写的新增到数据库的方法泛型是OmsOrderItem
        // 所以我们要编写代码将上集合转换为List<OmsOrderItem>集合
        List<OmsOrderItem> omsOrderItems=new ArrayList<>();
        // 遍历OrderItemAddDTO集合
        for(OrderItemAddDTO addDTO : itemAddDTOs ){
            // 先实例化一个OmsOrderItem对象,以备赋值使用
            OmsOrderItem orderItem=new OmsOrderItem();
            // 将同名属性赋值到orderItem对象中
            BeanUtils.copyProperties(addDTO,orderItem);
            // 将addDTO对象中没有的id和orderId属性赋值
            // 赋值Id
            Long itemId=IdGeneratorUtils.getDistributeId("order_item");
            orderItem.setId(itemId);
            // 赋值orderId
            orderItem.setOrderId(order.getId());
            // 将赋好值的对象添加到omsOrderItems集合中
            omsOrderItems.add(orderItem);
            // 第二部分:执行操作数据库的指令
            // 1.减少库存
            // 当前循环是订单中的一件商品,我们可以在此处对这个商品进行库存的减少
            // 当前对象属性中是包含skuId和要购买的商品数量的,所以可以执行库存的修改
            // 先获取skuId
            Long skuId=orderItem.getSkuId();
            // 修改库存是Dubbo调用的
            int rows=dubboSkuService.reduceStockNum(
                                        skuId,orderItem.getQuantity());
            // 判断rows(数据库受影响的行数)的值
            if(rows==0){
                log.warn("商品库存不足,skuId:{}",skuId);
                // 库存不足不能继续生成订单,抛出异常,终止事务进行回滚
                throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,
                        "库存不足!");
            }
            // 2.删除勾选的购物车商品
            OmsCart omsCart=new OmsCart();
            omsCart.setUserId(order.getUserId());
            omsCart.setSkuId(skuId);
            // 执行删除的方法
            omsCartService.removeUserCarts(omsCart);
        }
        // 3.执行新增订单
        // omsOrderMapper直接调用新增订单的方法即可
        omsOrderMapper.insertOrder(order);
        // 4.新增订单中所有商品的订单项信息
        // omsOrderItemMapper中编写了批量新增订单项的功能
        omsOrderItemMapper.insertOrderItemList(omsOrderItems);
        // 第三部分:准备返回值,返回给前端
        // 当前业务逻辑层方法返回值为OrderAddVO
        // 我们需要做的就是实例化这个对象,给它赋值并返回
        OrderAddVO addVO=new OrderAddVO();
        // 给addVO各属性赋值
        addVO.setId(order.getId());
        addVO.setSn(order.getSn());
        addVO.setCreateTime(order.getGmtOrder());
        addVO.setPayAmount(order.getAmountOfActualPay());
        // 最后千万别忘了返回addVO!!!!!
        return addVO;
    }

    // 为Order对象补全属性值的方法
    private void loadOrder(OmsOrder order) {
        // 本方法针对order对象没有被赋值的属性,进行生成或手动赋值
        // 给id赋值,订单业务不使用数据库自增列做id,而使用Leaf分布式序列生成系统
        Long id = IdGeneratorUtils.getDistributeId("order");
        order.setId(id);

        // 生成订单号,直接使用UUID即可
        order.setSn(UUID.randomUUID().toString());
        // 赋值UserId
        // 以后秒杀业务调用这个方法时,userId属性是会被赋值的
        // 所以这里要判断一下userId是否已经有值,没有值再赋值
        if (order.getUserId() == null) {
            // 从SpringSecurity上下文中获得当前登录用户id
            order.setUserId(getUserId());
        }

        // 为订单状态赋值
        // 订单状态如果为null ,将其设默认值0,表示未支付
        if (order.getState() == null){
            order.setState(0);
        }

        // 为了保证下单时间和数据创建时间和最后修改时间一致
        // 我们给他们赋相同的值
        LocalDateTime now=LocalDateTime.now();
        order.setGmtOrder(now);
        order.setGmtCreate(now);
        order.setGmtModified(now);

        // 验算实际支付金额
        // 计算公式:   实际支付金额=原价-优惠+运费
        // 数据类型使用BigDecimal,防止浮点偏移,还有更大的取值范围
        BigDecimal price=order.getAmountOfOriginalPrice();
        BigDecimal freight=order.getAmountOfFreight();
        BigDecimal discount=order.getAmountOfDiscount();
        BigDecimal actualPay=price.subtract(discount).add(freight);
        // 最后将计算完成的实际支付金额赋值给order
        order.setAmountOfActualPay(actualPay);

    }

    @Override
    public void updateOrderState(OrderStateUpdateDTO orderStateUpdateDTO) {

    }

    @Override
    public JsonPage<OrderListVO> listOrdersBetweenTimes(OrderListTimeDTO orderListTimeDTO) {
        return null;
    }

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        return null;
    }


    public CsmallAuthenticationInfo getUserInfo(){
        // 编写SpringSecurity上下文中获得用户信息的代码
        UsernamePasswordAuthenticationToken authenticationToken=
                (UsernamePasswordAuthenticationToken)
                        SecurityContextHolder.getContext().getAuthentication();
        // 为了逻辑严谨性,判断一下SpringSecurity上下文中的信息是不是null
        if(authenticationToken == null){
            throw new CoolSharkServiceException(
                    ResponseCode.UNAUTHORIZED,"您没有登录!");
        }
        // 确定authenticationToken不为null
        // 就可以从中获得用户信息了
        CsmallAuthenticationInfo csmallAuthenticationInfo=
                (CsmallAuthenticationInfo) authenticationToken.getCredentials();
        // 别忘了返回
        return csmallAuthenticationInfo;
    }
    // 业务逻辑层中的方法实际上都只需要用户的id即可
    // 我们可以再编写一个方法,从用户对象中获得id
    public Long getUserId(){
        return getUserInfo().getId();
    }
}
