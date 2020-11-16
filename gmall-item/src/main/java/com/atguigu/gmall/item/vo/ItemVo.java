package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {

    // 1.面包屑中需要的字段
    // 一二三级分类
    private List<CategoryEntity> categories;
    // 品牌
    private Long brandId;
    private String brandName;
    // spu
    private Long spuId;
    private String spuName;

    // 2.sku详细信息
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private Integer weight;
    private String defaultImage;
    // sku的图片列表
    private List<SkuImagesEntity> images;
    // 营销信息
    private List<ItemSaleVo> sales;
    // 库存信息
    private Boolean store = false;

    // 可供选择的销售属性及值
    // [{attrId: 4, attrName: 颜色, attrValues: ["暗夜黑", "白天白"]}，
    // {attrId: 5, attrName: 内存, attrValues: ["6G", "8G"]}]
    private List<SaleAttrValueVo> saleAttrs;

    // 当前sku的销售属性
    // {4: "暗夜黑", 5: "8G"}
    private Map<Long, String> saleAttr;

    // 销售属性组合和skuId的映射关系
    // {"暗夜黑,8G,128G": 100, "白天白,8G,256G": 101}
    private String skusJson;

    // 3.商品介绍
    private List<String> spuImages;
    // 规格与包装
    private List<GroupVo> groups;
}
