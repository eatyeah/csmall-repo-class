package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.front.service.IFrontProductService;
import cn.tedu.mall.pojo.product.vo.*;
import cn.tedu.mall.product.service.front.IForFrontAttributeService;
import cn.tedu.mall.product.service.front.IForFrontSkuService;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FrontProductServiceImpl implements IFrontProductService {

    @DubboReference
    private IForFrontSpuService dubboSpuService;
    // 根据spuId查询sku信息用的对象
    @DubboReference
    private IForFrontSkuService dubboSkuService;
    // 根据spuId查询查询当前商品的所有属性/规格用的对象
    @DubboReference
    private IForFrontAttributeService dubboAttributeService;


    // 根据分类id分页查询spu列表
    @Override
    public JsonPage<SpuListItemVO> listSpuByCategoryId(Long categoryId, Integer page, Integer pageSize) {
        // dubbo调用的方法是完成了分页查询的方法,这里只是调用然后返回即可
        JsonPage<SpuListItemVO> jsonPage=
                dubboSpuService.listSpuByCategoryId(categoryId, page, pageSize);
        // 返回jsonPage!!!
        return jsonPage;
    }

    // 根据spuId查询spu信息
    @Override
    public SpuStandardVO getFrontSpuById(Long id) {
        SpuStandardVO spuStandardVO=dubboSpuService.getSpuById(id);
        return spuStandardVO;
    }
    // 根据spuId查询对应的sku列表
    @Override
    public List<SkuStandardVO> getFrontSkusBySpuId(Long spuId) {
        List<SkuStandardVO> list=dubboSkuService.getSkusBySpuId(spuId);
        return list;
    }

    // 根据spuId查询spuDetail信息
    @Override
    public SpuDetailStandardVO getSpuDetail(Long spuId) {
        SpuDetailStandardVO spuDetailStandardVO=
                dubboSpuService.getSpuDetailById(spuId);
        return spuDetailStandardVO;
    }

    // 根据spuId查询对应的所有属性\规格列表
    @Override
    public List<AttributeStandardVO> getSpuAttributesBySpuId(Long spuId) {
        List<AttributeStandardVO> list=dubboAttributeService
                .getSpuAttributesBySpuId(spuId);
        return list;
    }
}
