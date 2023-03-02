package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.ResponseCode;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
                    (FrontCategoryTreeVO<FrontCategoryEntity>) redisTemplate.boundValueOps(CATEGORY_TREE_KEY).get();
            // 将查询出的数据返回
            return treeVO;
        }
        // Redis中没有三级分类树信息,表示本次请求可能是首次访问
        // 就需要从数据库中查询分类对象集合,再构建三级分类树,再保存到Redis的业务流程
        // dubbo调用查询所有分类对象的方法
        List<CategoryStandardVO> categoryStandardVOs = dubboCategoryService.getCategoryList();
        // 记住CategoryStandardVO是没有children属性的,FrontCategoryEntity是有的!
        // 下面需要编写一个方法,将子分类对象保存到对应的父分类对象的children属性中
        // 大概思路就是先将CategoryStandardVO转换为FrontCategoryEntity类型,然后再将父子分类关联
        // 整个转换和关联的过程比较复杂,我们编写一个方法来完成
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO = initTree(categoryStandardVOs);
        // 上面方法完成了三级分类树的构建,下面要将treeVO保存到Redis中
        redisTemplate.boundValueOps(CATEGORY_TREE_KEY).set(treeVO, 1, TimeUnit.MINUTES);
        // 上面时间定义了1分钟,是学习测试比较合适的,上线项目中,保存时间会比较长,例如24小时甚至更多
        // 最后也别忘了返回treeVO
        return treeVO;
    }

    private FrontCategoryTreeVO<FrontCategoryEntity> initTree(List<CategoryStandardVO> categoryStandardVOs) {
        // 确定所有父分类id
        // 以父分类的id为Key,以子分类对象为value保存在一个Map中
        // 一个父分类可以包含多个子分类对象,所以这个map的value是个list
        Map<Long, List<FrontCategoryEntity>> map = new HashMap<>();
        log.info("准备构建的三级分类树对象数量为：{}", categoryStandardVOs.size());
        // 遍历数据库查询出来的所有分类集合对象
        for (CategoryStandardVO categoryStandardVO : categoryStandardVOs) {
            // 因为CategoryStandardVO对象没有children属性,不能保存关联的子分类对象
            // 所以要将categoryStandardVO中的值赋值给能保存children属性的FrontCategoryEntity对象
            FrontCategoryEntity frontCategoryEntity = new FrontCategoryEntity();
            // 同名属性赋值
            BeanUtils.copyProperties(categoryStandardVO, frontCategoryEntity);
            // 获取当前分类对象的父分类id,用作map元素的key值(如果父分类id为0,就是一级分类)
            Long parentId = frontCategoryEntity.getParentId();
            // 判断这个父分类id是否已经存在于map
            if (!map.containsKey(parentId)) {
                // 如果map中没有当前遍历对象父分类id作为key的元素
                // 那么就要新建这个元素，就要确定key和value
                // key就是parentId,value是一个list,要实例化,而且list中还要保存当前正在遍历的对象
                List<FrontCategoryEntity> value = new ArrayList<>();
                value.add(frontCategoryEntity);
                // 最后将准备好的key和value保存到map中
                map.put(parentId, value);
            } else {
                // 如果map中已经有当前遍历对象父分类id作为key的元素
                map.get(parentId).add(frontCategoryEntity);
            }
        }
        // 第二步:
        // 将子分类对象添加到对应的父分类对象的childrens属性中
        // 先获取所有一级分类对象,也就是父分类id为0的对象
        List<FrontCategoryEntity> firstLevels = map.get(0L);
        // 判断一级分类集合如果为null(或没有元素),直接抛出异常,终止程序
        if (firstLevels == null || firstLevels.isEmpty()) {
            throw new CoolSharkServiceException(ResponseCode.INTERNAL_SERVER_ERROR, "没有一级分类对象");
        }
        // 遍历一级分类集合
        for (FrontCategoryEntity oneLevel : firstLevels) {
            // 一级分类对象的id就是二级分类对象的父id
            Long secondLevelParentId = oneLevel.getId();
            // 根据上面二级分类的父id,获得这个一级分类包含的所有二级分类对象集合
            List<FrontCategoryEntity> secondLevels = map.get(secondLevelParentId);
            // 判断二级分类中是否有元素
            if (secondLevels == null || secondLevels.isEmpty()) {
                // 二级分类缺失不用抛异常,日志输出警告即可
                log.warn("当前分类没有二级分类内容：{}", secondLevelParentId);
                // 如果二级分类对象缺失,可以直接跳过本次循环剩余内容,继续下次循环
                continue;
            }
            // 确定二级分类对象后,遍历二级分类对象集合
            for (FrontCategoryEntity twoLevel : secondLevels) {
                // 获取当前二级分类对象的id,作为三级分类的父id
                Long thirdLevelParentId = twoLevel.getId();
                // 根据这个id获得这个二级分类对象关联的所有三级分类对象集合
                List<FrontCategoryEntity> thirdLevels = map.get(thirdLevelParentId);
                // 再判断这个三级分类集合是否为null
                if (thirdLevels == null || thirdLevels.isEmpty()) {
                    log.warn("当前对象没有三级分类内容：{}", thirdLevelParentId);
                    continue;
                }
                // 将三级分类对象集合添加到关联的二级分类对象childrens属性中
                twoLevel.setChildrens(thirdLevels);
            }
            // 将二级分类对象集合添加到关联的一级分类对象childrens属性中
            oneLevel.setChildrens(secondLevels);
        }
        // 到此为止,所有的分类对象都应该确认了自己和父\子分类对象的关联关系
        // 最后我们要将一级分类的集合firstLevels,
        // 赋值给FrontCategoryTreeVO<FrontCategoryEntity>的list属性
        // 实例化对象
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO = new FrontCategoryTreeVO<>();
        treeVO.setCategories(firstLevels);
        // 最后千万别忘了返回  treeVO!!!!
        return treeVO;
    }
}





