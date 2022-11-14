package cn.tedu.mall.search.repository;

import cn.tedu.mall.pojo.search.entity.SpuEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpuEntityRepository extends
                                ElasticsearchRepository<SpuEntity,Long> {
    // 根据用户输入的关键字,查询ES中匹配的商品
    // Logstash将所有商品spu的信息中需要分词的字段,拼接成了一个search_text字段
    // 因为SpuEntity实体类中没有创建searchText字段,所有只能通过查询语句完成
    @Query("{\"match\":{\"search_text\":{\"query\":\"?0\"}}}")
    Page<SpuEntity> querySearchByText(String keyword, Pageable pageable);

}
