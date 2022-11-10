package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.service.ISeckillService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

    @Override
    public SeckillCommitVO commitSeckill(SeckillOrderAddDTO seckillOrderAddDTO) {
        return null;
    }
}
