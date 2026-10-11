package com.zyyqq.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 编辑用户自定义食物请求（仅本人可改）。
 * 字段均可选，只更新传入的字段。
 */
@Data
public class UpdateCustomFoodRequest {

    private String name;

    private Long categoryId;

    /** 每 100g 热量（组合食谱不可改） */
    private BigDecimal calories;

    private BigDecimal protein;

    private BigDecimal carbohydrate;

    private BigDecimal fat;

    private BigDecimal fiber;

    /** 常用计量单位名称，如 个/份 */
    private String unitName;

    /** 每个/每份约多少克 */
    private BigDecimal unitWeight;
}
