package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.front.service.IFrontCategoryService;
import cn.tedu.mall.pojo.front.entity.FrontCategoryEntity;
import cn.tedu.mall.pojo.front.vo.FrontCategoryTreeVO;
import cn.tedu.mall.pojo.product.vo.CategoryStandardVO;
import cn.tedu.mall.product.service.front.IForFrontCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@DubboService
@Service
@Slf4j
public class FrontCategoryServiceImpl implements IFrontCategoryService {

    // 装配Dubbo业务逻辑层对象,完成Product模块查询全部分类对象集合的方法
    // front模块不连数据,是消费者
    @DubboReference
    private IForFrontCategoryService dubboCategoryService;
    // 装配操作Redis的对象
    @Autowired
    private RedisTemplate redisTemplate;

    // 开发过程中,使用Redis的规范要求所有代码中使用的Redis的Key,都要定义为常量避免拼写错误
    public static final String CATEGORY_TREE_KEY="category_tree";

    @Override
    public FrontCategoryTreeVO categoryTree() {
        // 方法中先检查Redis中是否保存了三级分类树对象
        if(redisTemplate.hasKey(CATEGORY_TREE_KEY)){
            // redis中如果已经保存了这个key,直接获取
            FrontCategoryTreeVO<FrontCategoryEntity> treeVO=
                    (FrontCategoryTreeVO<FrontCategoryEntity>)
                    redisTemplate.boundValueOps(CATEGORY_TREE_KEY).get();
            // 将从redis中获取的treeVO返回
            return treeVO;
        }
        // Redis中没有三级分类树信息,表示本次访问可以是首次访问
        // 就要进行连接数据库查询数据后,构建三级分类树结构,再保存到Redis的业务流程
        // dubbo调用查询所有分类对象的方法
        List<CategoryStandardVO> categoryStandardVOs=
                            dubboCategoryService.getCategoryList();
        // 请记住CategoryStandardVO是没有children属性的,FrontCategoryEntity是有的!
        // 下面就是要编写一个方法,将子分类对象保存到对应的父分类对象的children属性中
        // 所有大概思路就是将CategoryStandardVO转换为FrontCategoryEntity
        // 转换和构建过程比较复杂,我们专门编写一个类来完成
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO=
                                            initTree(categoryStandardVOs);

        return null;
    }

    private FrontCategoryTreeVO<FrontCategoryEntity> initTree(List<CategoryStandardVO> categoryStandardVOs) {

        return null;
    }
}
