package cn.tedu.mall.search.test;

import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.search.repository.SpuForElasticRepository;
import cn.tedu.mall.search.service.ISearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 下面注解必须加!!!!!!
@SpringBootTest
public class SpuElasticTest {

    @Autowired
    private ISearchService searchService;

    @Test
    void loadData(){
        searchService.loadSpuByPage();
        System.out.println("ok");
    }

    @Autowired
    private  SpuForElasticRepository spuRepository;
    @Test
    void showData(){
        Iterable<SpuForElastic> spus=spuRepository.findAll();
        spus.forEach(spu -> System.out.println(spu));
    }

    @Test
    void getSpuByTitle(){
        // 根据title指定的分词查询数据
        Iterable<SpuForElastic> spus=
                spuRepository.querySpuForElasticsByTitleMatches("华为");
        spus.forEach(spu -> System.out.println(spu));

    }



}
