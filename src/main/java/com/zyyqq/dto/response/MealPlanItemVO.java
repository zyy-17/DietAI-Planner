package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 方案里的一条食物条目（展示用）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanItemVO {

    private Long id;
    private Long foodId;
    private String foodSource;
    private String foodName;
    private BigDecimal amount;
    /** 展示用份量，如「1个」「250ml」，没有单位信息时退化成「50g」 */
    private String amountText;
    private BigDecimal calories;
    private BigDecimal protein;
    private BigDecimal carbohydrate;
    private BigDecimal fat;
}
