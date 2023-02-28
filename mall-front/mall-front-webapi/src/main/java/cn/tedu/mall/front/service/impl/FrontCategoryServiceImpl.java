package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.front.service.IFrontCategoryService;
import cn.tedu.mall.pojo.front.entity.FrontCategoryEntity;
import cn.tedu.mall.pojo.front.vo.FrontCategoryTreeVO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @Description:
 * @Author: sweeterjava@163.com
 * @Date: 2023/2/28
 * @Time: 14:42
 */

@Service
@Slf4j
public class FrontCategoryServiceImpl implements IFrontCategoryService {

    // front模块要dubbo调用product模块的方法,实现查询所有分类对象集合
    @DubboReference
    private IFrontCategoryService frontCategoryService;

    // 装配操作redis的对象
    @Autowired
    private RedisTemplate redisTemplate;

    // 开发过程中,使用Redis时规范要求需要定义一个常量来作为Redis的key,避免拼写错误
    public static final String CATEGORY_TREE_KEY = "category_tree";

    @Override
    public FrontCategoryTreeVO categoryTree() {
        // 先从redis中查询是否存在三级分类的树形结构数据
        if (redisTemplate.hasKey(CATEGORY_TREE_KEY)) {
            // 如果存在,直接从redis中获取
            FrontCategoryTreeVO<FrontCategoryEntity> treeVO =
                    (FrontCategoryTreeVO<FrontCategoryEntity>)
                            redisTemplate.boundValueOps(CATEGORY_TREE_KEY).get();
        }

        return null;
    }
}
