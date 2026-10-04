package com.zyyqq.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新建 / 修改食谱的基本信息（不再走 AI 生成，全部由用户自己填）。
 */
@Data
public class MealPlanSaveRequest {

    /** 食谱名，为空时按「N天目标食谱」自动生成 */
    private String name;

    /** 膳食目标：lose 减脂 / gain 增重 / maintain 维持体重 */
    private String goal;

    /** 周期天数：3 / 7 / 14 */
    private Integer days;

    /** 每日餐次：breakfast / lunch / dinner / snack */
    private List<String> meals;

    /** 每日目标热量，可空（实际热量按食物库累计） */
    private BigDecimal dailyCalories;

    /** 一句话概述 */
    private String summary;
}
