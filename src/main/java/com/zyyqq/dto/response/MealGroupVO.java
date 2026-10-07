package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** 某一餐（早餐/午餐/…）的食物清单与小计。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealGroupVO {

    private String mealType;
    /** 中文餐次名，如「早餐」 */
    private String mealLabel;
    private BigDecimal calories;
    private List<MealPlanItemVO> items;
}
