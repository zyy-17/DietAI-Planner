package com.zyyqq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 记录饮食时添加自定义食物（私有，仅创建者本人可见）。
 * 热量/蛋白质/碳水/脂肪均为每 100g 的数值。
 */
@Data
public class AddCustomDietRecordRequest {

    @NotBlank(message = "食物名称不能为空")
    private String foodName;

    /** 可选，未指定时归入"其他"分类 */
    private Long categoryId;

    @NotNull(message = "热量不能为空")
    private BigDecimal calories;

    private BigDecimal protein;

    private BigDecimal carbohydrate;

    private BigDecimal fat;

    private BigDecimal fiber;

    @NotNull(message = "餐次不能为空")
    private String mealType;

    @NotNull(message = "食用份量不能为空")
    private BigDecimal amount;
}
