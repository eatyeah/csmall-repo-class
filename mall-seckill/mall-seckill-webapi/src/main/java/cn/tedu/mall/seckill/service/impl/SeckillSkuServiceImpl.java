package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import cn.tedu.mall.pojo.seckill.vo.SeckillSkuVO;
import cn.tedu.mall.product.service.seckill.IForSeckillSkuService;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.service.ISeckillSkuService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SeckillSkuServiceImpl implements ISeckillSkuService {
    @Autowired
    private SeckillSkuMapper skuMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    // Dubbo调用获得sku的常规信息
    @DubboReference
    private IForSeckillSkuService dubboSkuService;

    // 根据秒杀spuId查询秒杀sku列表
    @Override
    public List<SeckillSkuVO> listSeckillSkus(Long spuId) {
        // 执行查询,根据spuId查询秒杀的sku列表
        List<SeckillSku> seckillSkus=skuMapper.findSeckillSkusBySpuId(spuId);
        // 当前方法的返回值List<SeckillSkuVO> 泛型SeckillSkuVO
        // 也是既包含秒杀信息也包含常规信息的sku对象,所以我们先实例化这个集合,
        // 以备下面循环中为它添加元素
        List<SeckillSkuVO> seckillSkuVOs=new ArrayList<>();
        // 遍历从数据库查询出来的所有sku
        for(SeckillSku sku : seckillSkus){
            // 先获取skuId,方便后面代码调用
            Long skuId=sku.getSkuId();
            // 声明sku对象的key
            // mall:seckill:sku:vo:1
            String seckillSkuVOKey= SeckillCacheUtils.getSeckillSkuVOKey(skuId);
            // 声明SeckillSkuVO类型对象,先赋null
            SeckillSkuVO seckillSkuVO=null;
            // 判断Redis中是否包含这个key
            if(redisTemplate.hasKey(seckillSkuVOKey)){
                seckillSkuVO=(SeckillSkuVO)redisTemplate
                        .boundValueOps(seckillSkuVOKey).get();
            }else{
                // Redis中没有这个sku,就要到数据库中去查询了
            }

        }
        return null;
    }
}
