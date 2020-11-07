package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

/**
 * search.gmall.com/search?keyword=手机&brandId=1,2&props=4:8G-12G&props=5:128G-256G&sort=1
 *  &priceFrom=1000&priceTo=2000&store=true&pageNum=1
 */
@Data
public class SearchParamVo {

    // 搜索关键字
    private String keyword;
    // 品牌过滤条件
    private List<Long> brandId;
    // 分类的过滤条件
    private List<Long> categoryId;

    // 规格参数的过滤：props=4:8G-12G&props=5:128G-256G
    private List<String> props;

    // 排序字段：1-价格升序 2-价格降序 3-销量降序 4-新品降序  默认0，使用得分排序
    private Integer sort;

    // 价格区间过滤条件
    private Double priceTo;
    private Double priceFrom;

    private Boolean store; // 是否有货

    private Integer pageNum = 1; // 页码，默认第一页
    private final Integer pageSize = 20; // 每页大小
}
