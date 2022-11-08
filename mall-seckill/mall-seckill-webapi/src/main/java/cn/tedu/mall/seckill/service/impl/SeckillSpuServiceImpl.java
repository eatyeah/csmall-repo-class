package cn.tedu.mall.seckill.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.product.vo.SpuDetailStandardVO;
import cn.tedu.mall.pojo.product.vo.SpuStandardVO;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuDetailSimpleVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuVO;
import cn.tedu.mall.product.service.seckill.IForSeckillSpuService;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.service.ISeckillSpuService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SeckillSpuServiceImpl implements ISeckillSpuService {
    //  装配查询秒杀Spu列表的Mapper
    @Autowired
    private SeckillSpuMapper seckillSpuMapper;

    // 查询方法的返回值泛型为SeckillSpuVO,其中包含很多普通spu表中的信息
    // 所以我们还要在代码中Dubbo调用根据spuId查询普通spu信息的方法,还是product模块
    @DubboReference
    private IForSeckillSpuService dubboSeckillSpuService;

    // 分页查询秒杀商品信息
    // 这个方法的返回值泛型是SeckillSpuVO,其中包含了常规spu信息和秒杀spu信息
    // 所以这个方法中我们要先查询秒杀信息,在查询常规信息,最后将信息整合一起返回
    @Override
    public JsonPage<SeckillSpuVO> listSeckillSpus(Integer page, Integer pageSize) {
        // 设置分页条件,准备分页查询
        PageHelper.startPage(page,pageSize);
        // 然后进行查询,获得分页数据
        List<SeckillSpu> seckillSpus=seckillSpuMapper.findSeckillSpus();
        // 我们要实例化一个SeckillSpuVO泛型的集合,以备赋值和最后的返回
        List<SeckillSpuVO> seckillSpuVOs=new ArrayList<>();
        // 遍历从数据库中查询出的所有秒杀商品列表
        for(SeckillSpu seckillSpu : seckillSpus){
            // 获得秒杀商品的spuId
            Long spuId=seckillSpu.getSpuId();
            // 根据这个SpuId利用dubbo去查询这个spu的常规信息
            SpuStandardVO spuStandardVO=dubboSeckillSpuService.getSpuById(spuId);
            // 秒杀信息在seckillSpu中,常规信息在spuStandardVO中
            // 下面就是要将上面两个对象的属性都赋值到SeckillSpuVO对象中
            SeckillSpuVO seckillSpuVO=new SeckillSpuVO();
            // 常规信息都在spuStandardVO中,所以直接将同名属性赋值
            BeanUtils.copyProperties(spuStandardVO,seckillSpuVO);
            // 秒杀信息要单独赋值,因为不全是同名属性
            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice());
            seckillSpuVO.setStartTime(seckillSpu.getStartTime());
            seckillSpuVO.setEndTime(seckillSpu.getEndTime());
            // 到此为止seckillSpuVO包含了常规spu信息和秒杀spu信息
            // 将这个对象保存到集合
            seckillSpuVOs.add(seckillSpuVO);
        }
        // 最后别忘了返回
        return JsonPage.restPage(new PageInfo<>(seckillSpuVOs));
    }

    // 根据spuId查询spu详情(返回值包含常规信息和秒杀信息已经随机码)
    @Override
    public SeckillSpuVO getSeckillSpu(Long spuId) {
        // 在最终完整版的代码中,要在这里添加布隆过滤器的判断
        // 执行布隆过滤器判断spuId是否存在,如果不存在直接抛出异常

        // 检查spu信息是否已经在Redis中,还是先定Redis的key
        String seckillSpuKey= SeckillCacheUtils.getSeckillSpuVOKey(spuId);
        // 先声明一个当前方法的返回值类型对象为null
        SeckillSpuVO seckillSpuVO=null;
        // 判断Redis是否包含key
        if(redisTemplate.hasKey(seckillSpuKey)){
            // 如果包含这个key直接从redis 中获取
            seckillSpuVO=(SeckillSpuVO)redisTemplate
                    .boundValueOps(seckillSpuKey).get();
        }else{
            // redis中没有这个key,完成数据库查询
            // 先根据spuId查询秒杀信息
            SeckillSpu seckillSpu=seckillSpuMapper.findSeckillSpuById(spuId);
            // 判断seckillSpu是否为null(因为布隆过滤器可能产生误判)
            if(seckillSpu == null){
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,
                        "您访问的商品不存在!");
            }
            // 到此为止,我们已经查询出了spu的秒杀商品信息,下面还要获取常规商品信息
            // dubbo调用product模块的方法获取spu常规信息
            SpuStandardVO spuStandardVO= dubboSeckillSpuService
                                                        .getSpuById(spuId);
            seckillSpuVO=new SeckillSpuVO();
            // 将常规信息中的同名属性赋值到seckillSpuVO对象中
            BeanUtils.copyProperties(spuStandardVO,seckillSpuVO);
            // 将秒杀信息也赋值到seckillSpuVO
            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice());
            seckillSpuVO.setStartTime(seckillSpu.getStartTime());
            seckillSpuVO.setEndTime(seckillSpu.getEndTime());
            // seckillSpuVO对象保存到Redis中
            redisTemplate.boundValueOps(seckillSpuKey).set(
                    seckillSpuVO,10*60*1000+RandomUtils.nextInt(10000),
                    TimeUnit.MILLISECONDS);
        }



        return null;
    }

    // 装配操作Redis的对象
    @Autowired
    private RedisTemplate redisTemplate;

    // 当前项目中没有定义spuDetail对应Redis的key的常量
    // 要么自己去添加, 要么这里定义一个   PREFIX是"前缀"的意思
    public static final String
            SECKILL_SPU_DETAIL_VO_PREFIX="seckill:spu:detail:vo:";

    // 查询秒杀spu信息的spuDetail
    @Override
    public SeckillSpuDetailSimpleVO getSeckillSpuDetail(Long spuId) {
        // 使用常量拼接spuId获得Redis的key
        String seckillSpuDetailKey=SECKILL_SPU_DETAIL_VO_PREFIX+spuId;
        // 先声明一个返回值类型的对象,设值为null即可
        // 因为后面判断无论Redis中有没有这个key,都需要使用这个对象
        SeckillSpuDetailSimpleVO simpleVO=null;
        // 判断Redis中是否有这个Key
        if(redisTemplate.hasKey(seckillSpuDetailKey)){
            // 如果 key已经存在,直接从redis中获取即可
            simpleVO=(SeckillSpuDetailSimpleVO)redisTemplate
                    .boundValueOps(seckillSpuDetailKey).get();
        }else{
            // 如果Redis中没有这个key,就要从数据库中查询
            // 使用Dubbo调用product模块的方法查询即可
            SpuDetailStandardVO spuDetailStandardVO =
                    dubboSeckillSpuService.getSpuDetailById(spuId);
            // 实例化SeckillSpuDetailSimpleVO对象
            simpleVO=new SeckillSpuDetailSimpleVO();
            BeanUtils.copyProperties(spuDetailStandardVO,simpleVO);
            // simpleVO赋值完成后,将它保存到Redis中,以便后续请求获取
            redisTemplate.boundValueOps(seckillSpuDetailKey)
                    .set(simpleVO,10*60*1000+ RandomUtils.nextInt(10000),
                            TimeUnit.MILLISECONDS);
        }
        // 返回simpleVO
        return simpleVO;
    }
}
