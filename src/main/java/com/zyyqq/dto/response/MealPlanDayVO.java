package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** 方案里的一天：按餐分组的食物 + 当日营养合计。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanDayVO {

    /** 第几天，从 1 开始 */
    private Integer dayIndex;
    private BigDecimal calories;
    private BigDecimal protein;
    private BigDecimal carbohydrate;
    private BigDecimal fat;
    private List<MealGroupVO> meals;
}
