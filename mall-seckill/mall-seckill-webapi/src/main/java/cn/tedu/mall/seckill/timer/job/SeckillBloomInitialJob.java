package cn.tedu.mall.seckill.timer.job;

import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.utils.RedisBloomUtils;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

public class SeckillBloomInitialJob implements Job {

    // 装配Redis操作布隆过滤器的对象
    @Autowired
    private RedisBloomUtils redisBloomUtils;
    // 装配连接数据库,查询所有秒杀商品spuId的mapper
    @Autowired
    private SeckillSpuMapper spuMapper;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 这是预热的方法,会在秒杀开始前5分钟运行
        // 首先查询下个批次所有的spuId
        // 如果有需求加载更多批次的数据,就应该查询更多批次,都添加到布隆过滤器
        // 我们的设计为假设秒杀一天一个批次,提前加载两个批次的数据
        // 获取今天的日期做布隆过滤器的key就可以
        // spu:bloom:filter:2022-11-14
        String bloomTodayKey=
                SeckillCacheUtils.getBloomFilterKey(LocalDate.now());
        // 获取明天的key
        String bloomTomorrowKey=
                SeckillCacheUtils.getBloomFilterKey(LocalDate.now().plusDays(1));
        // 实际开发时,应该调用两次查询,分别查询今天和明天添加秒杀商品的spuId数组
        // 分别保存在上面声明的不同的key中
        // 但是我们以学习布隆过滤器为目标,数据库中数据又少,所以暂时将数据库中所有数据都查询出来
        Long[] spuIds=spuMapper.findAllSeckillSpuIds();
        // 布隆过滤器的Api只支持保存String数组,而我们现在是Long数组
        // 需要进行转换,下面是转换操作
        String[] spuIdsStr=new String[spuIds.length];
        // 遍历spuIds数组,将其中元素转换为String类型,添加到spuIdsStr中
        for(int i=0;i<spuIds.length;i++){
            spuIdsStr[i]=spuIds[i]+"";
        }
        // 需要注意,我们如果查多个批次,每个批次都需要进行上面的转换工作
        // 学习过程中,因为数据都相同,所以与两个批次保存相同的商品
        redisBloomUtils.bfmadd(bloomTodayKey,spuIdsStr);
        redisBloomUtils.bfmadd(bloomTomorrowKey,spuIdsStr);
        System.out.println("两个批次的布隆过滤器加载完毕");

    }
}
