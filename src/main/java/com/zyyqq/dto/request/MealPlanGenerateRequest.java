package com.zyyqq.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AI 智能生成食谱请求。
 *
 * <p>用户填写想法/目标/周期等参数，后端创建空食谱后调用 ai-service
 * 生成完整 N 天膳食计划，再逐条写入 meal_plan_item。</p>
 */
@Data
public class MealPlanGenerateRequest {

    /** 膳食目标：lose 减脂 / gain 增重 / maintain 维持体重 */
    private String goal;

    /** 计划周期（天）：3 / 7 */
    private Integer days;

    /** 每日餐次：breakfast / lunch / dinner / snack */
    private List<String> meals;

    /**
     * 用户自由描述的想法/需求，如"我想吃中式减脂餐，喜欢鸡胸肉和西兰花，不吃香菜"。
     * 可空，AI 会根据 goal 给出通用方案。
     */
    private String idea;

    /** 每日目标热量（kcal），可空 */
    private BigDecimal dailyCalories;

    /** 忌口食物列表，如 ["香菜", "芹菜"] */
    private List<String> dislikes;

    /** 饮食偏好标签，如 ["高蛋白", "低糖"] */
    private List<String> preferences;
}
