package cn.tedu.mall.front.controller;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.front.service.IFrontCategoryService;
import cn.tedu.mall.pojo.front.entity.FrontCategoryEntity;
import cn.tedu.mall.pojo.front.vo.FrontCategoryTreeVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description:
 * @Author: sweeterjava@163.com
 * @Date: 2023/2/28
 * @Time: 16:19
 */

@RestController
@RequestMapping("/front/category")
public class CategoryController {

    @Autowired
    private IFrontCategoryService categoryService;

    @GetMapping("/all")
    @ApiOperation("查询获取二级分类树对象")
    public JsonResult<FrontCategoryTreeVO<FrontCategoryEntity>> getTreeVO(){
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO = categoryService.categoryTree();
        return JsonResult.ok(treeVO);
    }

}
