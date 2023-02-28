package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.front.service.IFrontCategoryService;
import cn.tedu.mall.pojo.front.entity.FrontCategoryEntity;
import cn.tedu.mall.pojo.front.vo.FrontCategoryTreeVO;
import cn.tedu.mall.pojo.product.vo.CategoryStandardVO;
import cn.tedu.mall.product.service.front.IForFrontCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private IForFrontCategoryService dubboCategoryService;

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
            return treeVO;
        }
        List<CategoryStandardVO> categoryStandardVOs = dubboCategoryService.getCategoryList();

        FrontCategoryTreeVO<FrontCategoryEntity> treeVO = initTree(categoryStandardVOs);

        return null;
    }

    private FrontCategoryTreeVO<FrontCategoryEntity> initTree(List<CategoryStandardVO> categoryStandardVOs) {
        // 确定所有父分类id
        // 以父分类的id为Key,以子分类对象为value保存在一个Map中
        // 一个父分类可以包含多个子分类对象,所以这个map的value是个list
        Map<Long, List<FrontCategoryEntity>> map = new HashMap<>();
        log.info("准备构建的三级分类树对象数量为：{}",categoryStandardVOs.size());
        // 遍历数据库查询出来的所有分类集合对象
        for (CategoryStandardVO categoryStandardVO : categoryStandardVOs){
            // 因为CategoryStandardVO对象没有children属性,不能保存关联的子分类对象
            // 所以要将categoryStandardVO中的值赋值给能保存children属性的FrontCategoryEntity对象
            FrontCategoryEntity frontCategoryEntity = new FrontCategoryEntity();
            // 同名属性赋值
            BeanUtils.copyProperties(categoryStandardVO, frontCategoryEntity);
        }
        return null;
    }
}
