package cn.tedu.mall.search.repository;

import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

// SpuForElastic实体类操作ES的持久层接口
// 需要继承SpringData给定的父接口,继承之后可以直接使用提供的基本增删改查方法
@Repository
public interface SpuForElasticRepository extends
                            ElasticsearchRepository<SpuForElastic,Long> {


}
