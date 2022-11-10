package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderItemAddDTO;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.model.Success;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.config.RabbitMqComponentConfiguration;
import cn.tedu.mall.seckill.service.ISeckillService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SeckillServiceImpl implements ISeckillService {

    // 秒杀业务中,使用Redis的代码都是在判断数值,直接使用字符串类型的Redis对象即可
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    // 需要dubbo调用mall_order模块的普通订单的生成业务
    @DubboReference
    private IOmsOrderService dubboOrderService;
    // 需要将秒杀成功信息发送给消息队列
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /*
    1.判断用户是否为重复购买和Redis中该Sku是否有库存
    2.秒杀订单转换成普通订单,需要使用dubbo调用order模块的生成订单方法
    3.使用消息队列(RabbitMQ)将秒杀成功记录信息保存到success表中
    4.秒杀订单信息返回
     */
    @Override
    public SeckillCommitVO commitSeckill(SeckillOrderAddDTO seckillOrderAddDTO) {
        // 第一阶段:判断用户是否为重复购买和Redis中该Sku是否有库存
        // 从方法的参数中,获得用户想要购买的skuId
        Long skuId=seckillOrderAddDTO.getSeckillOrderItemAddDTO().getSkuId();
        // 从SpringSecurity上下文中获取登录用户的Id
        Long userId=getUserId();
        // 我们明确了本次请求时哪个用户要购买哪个商品(userId以及skuId的具体值)
        // 根据我们的秒杀业务规定,每个用户只能购买skuId一次
        // 所以可以依据userId和skuId生成检查重复购买的Key
        // mall:seckill:reseckill:2:1
        String reSeckillCheckKey= SeckillCacheUtils.getReseckillCheckKey(skuId,userId);
        // 以上面字符串为key,调用stringRedisTemplate中的increment方法
        // increment是增长的意思
        // 1.如果上面的key从没在Redis中保存过,redis中会创建这个key,并保存它的值为1
        // 2.如果上面的key已经在redis中,那么就会在当前的值基础上加1,再保存,如果当前是1,运行后变为2
        // 3.无论存不存在这个key,运行后都会返回值
        // 所以如果返回的值为1,就证明这个用户之前从未购买过这个商品
        Long seckillTimes=stringRedisTemplate
                .boundValueOps(reSeckillCheckKey).increment();
        // 如果seckillTimes值为1就可以购买,大于1就不能购买
        if(seckillTimes>100){
            // 抛出异常,提示不能重复购买,终止程序
            throw new CoolSharkServiceException(ResponseCode.FORBIDDEN,
                    "您已经购买过这个商品了,谢谢您的支持!");
        }
        // 程序运行到此处,表示用户第一次购买这个商品
        // 然后去检查是否有库存
        // 确定要购买的skuId,生成获得这个skuId已经预热的库存数的key
        // mall:seckill:sku:stock:1
        String skuStockKey=SeckillCacheUtils.getStockKey(skuId);
        // 根据上面的key在redis中调用decrement(减少)的方法,将库存数-1后返回
        Long leftStock=stringRedisTemplate
                .boundValueOps(skuStockKey).decrement();
        // leftStock是decrement方法减1之后返回的
        // 所以我们获得的leftStock是如果当前用户购买成功剩余的库存数
        // 如果leftStock是0,表示当前用户买到最后一个,如果小于0,才是没有库存,表示售罄
        if(leftStock<0){
            // 如果已经没有库存,就要终止本次用户购买
            // 要将当前用户购买此商品的次数恢复为0,下次该用户还能购买
            stringRedisTemplate.boundValueOps(reSeckillCheckKey).decrement();
            // 抛出异常
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,
                    "对不起,您要购买的商品暂时售罄");
        }
        // 到此为止,当前用户经过了重复购买的检查和库存的判断,可以开始生成订单了
        // 第二阶段:秒杀订单转换成普通订单,需要使用dubbo调用order模块的生成订单方法
        // 当前方法参数:SeckillOrderAddDTO,dubbo调用需要的参数:OrderAddDTO
        // 下面开始转换,转换代码较多,单独编写一个方法
        OrderAddDTO orderAddDTO=convertSeckillOrderToOrder(seckillOrderAddDTO);
        // 完成了转换,普通订单信息所有主要属性,都赋值完毕了
        // 在dubbo调用发送之前,最后将用户Id赋值到orderAddDTO
        orderAddDTO.setUserId(userId);
        // 使用dubbo调用生成订单的方法
        OrderAddVO orderAddVO=dubboOrderService.addOrder(orderAddDTO);
        // 第三阶段:使用消息队列(RabbitMQ)将秒杀成功记录信息保存到success表中
        // 业务要求我们记录秒杀成功的信息,但是它不是迫切运行的,所以使用消息队列完成
        // 我们要创建Success秒杀成功记录对象,然后将这个对象发送给RabbitMQ
        // 另寻时机编写处理消息队列的类和方法即可
        Success success=new Success();
        // Success大部分属性和秒杀sku信息重叠,可以使用秒杀订单项对象,给他同名属性赋值
        BeanUtils.copyProperties(seckillOrderAddDTO.getSeckillOrderItemAddDTO(),
                                    success);
        // 把确实的信息补齐
        success.setUserId(userId);
        success.setOrderSn(orderAddVO.getSn());
        success.setSeckillPrice(
                        seckillOrderAddDTO.getSeckillOrderItemAddDTO().getPrice());
        // 将赋值完备的success对象发送给RabbitMQ
        rabbitTemplate.convertAndSend(
                RabbitMqComponentConfiguration.SECKILL_EX,
                RabbitMqComponentConfiguration.SECKILL_RK,
                success);
        // 下面无需关注消息队列的后续处理,直接秒杀订单的后续工作即可
        // 第四阶段:秒杀订单信息返回
        // 当前方法要求返回SeckillCommitVO类型对象
        // 观察这个类中的属性,和OrderAddVO类属性完全一致,所以可以直接赋值之后返回
        SeckillCommitVO commitVO=new SeckillCommitVO();
        BeanUtils.copyProperties(orderAddVO,commitVO);
        // 别忘了返回commitVO
        return commitVO;
    }
    // 秒杀订单,转换为普通定的方法
    private OrderAddDTO convertSeckillOrderToOrder(SeckillOrderAddDTO seckillOrderAddDTO) {
        // 先实例化返回值类型对象
        OrderAddDTO orderAddDTO=new OrderAddDTO();
        // 将同名属性赋值到orderAddDTO对象
        BeanUtils.copyProperties(seckillOrderAddDTO,orderAddDTO);
        // 这两个订单对象基本属性都相同,区别在于
        // orderAddDTO普通订单对象,其中的订单项是一个集合:List<OrderItemAddDTO>
        // seckillOrderAddDTO秒杀订单对象,其中的秒杀订单项是一个对象:SeckillOrderItemAddDTO
        // 我们需要先获得一个普通订单的订单项对象
        OrderItemAddDTO orderItemAddDTO=new OrderItemAddDTO();
        BeanUtils.copyProperties(seckillOrderAddDTO.getSeckillOrderItemAddDTO(),
                                    orderItemAddDTO);
        // 实例化orderAddDTO对象中需要的普通订单项集合
        List<OrderItemAddDTO> list=new ArrayList<>();
        // 将赋值好的普通订单项对象添加到list集合中
        list.add(orderItemAddDTO);
        // 将普通订单项集合,赋值给OrderAddDTO
        orderAddDTO.setOrderItems(list);
        // 别忘了返回正确的对象!
        return orderAddDTO;
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
