package com.zyyqq.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 往食谱里添加一条食物（第几天 · 哪一餐 · 什么食物 · 多少克）。
 */
@Data
public class MealPlanItemAddRequest {

    /** 第几天，从 1 开始 */
    private Integer dayIndex;

    /** 餐次：breakfast / lunch / dinner / snack */
    private String mealType;

    private Long foodId;

    /** 食物来源：system 食物库 / user 本人自定义食物 */
    private String foodSource;

    /** 克数 */
    private BigDecimal amount;
}
