package cn.tedu.mall.search.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.search.entity.SpuEntity;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.search.repository.SpuEntityRepository;
import cn.tedu.mall.search.service.ISearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

// 实现查询远程服务器(虚拟机Linux)ELK系统中的信息(模糊查询spu商品)
@Service
@Slf4j
public class SearchRemoteServiceImpl implements ISearchService {

    // 装配新创建的持久层接口,对应spuEntity实体类
    @Autowired
    private SpuEntityRepository spuEntityRepository;

    @Override
    public JsonPage<SpuEntity> search(String keyword, Integer page, Integer pageSize) {
        // 调用查询es中根据关键字获取匹配商品spu信息的方法
        Page<SpuEntity> spus=spuEntityRepository.querySearchByText(
                            keyword, PageRequest.of(page-1,pageSize));

        JsonPage<SpuEntity> jsonPage=new JsonPage<>();
        // 分页信息
        jsonPage.setPage(page);
        jsonPage.setPageSize(pageSize);
        jsonPage.setTotal(spus.getTotalElements());
        jsonPage.setTotalPage(spus.getTotalPages());
        // 分页数据
        jsonPage.setList(spus.getContent());
        // 别忘了返回!!!
        return jsonPage;
    }

    // logStash自动同步,无需编写下面方法
    @Override
    public void loadSpuByPage() { }
}






