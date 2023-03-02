package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.front.service.IFrontProductService;
import cn.tedu.mall.pojo.product.vo.*;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import org.apache.dubbo.config.annotation.DubboReference;

import java.util.List;

/**
 * @Description:
 * @Author: sweeterjava@163.com
 * @Date: 2023/3/2
 * @Time: 15:32
 */
public class FrontProductServiceImpl implements IFrontProductService {

    @DubboReference
    private IForFrontSpuService dubboSpuService;

    // 根据分类id分页查询spu列表
    @Override
    public JsonPage<SpuListItemVO> listSpuByCategoryId(Long categoryId, Integer page, Integer pageSize) {
        // dubbo调用的方法是product模块编写的业务逻辑层方法
        // 这个方法中实际上完成了分页的操作,我们只需调用即可
        JsonPage<SpuListItemVO> jsonPage = dubboSpuService.listSpuByCategoryId(categoryId, page, pageSize);
        return jsonPage;
    }

    @Override
    public SpuStandardVO getFrontSpuById(Long id) {
        return null;
    }

    @Override
    public List<SkuStandardVO> getFrontSkusBySpuId(Long spuId) {
        return null;
    }

    @Override
    public SpuDetailStandardVO getSpuDetail(Long spuId) {
        return null;
    }

    @Override
    public List<AttributeStandardVO> getSpuAttributesBySpuId(Long spuId) {
        return null;
    }
}
