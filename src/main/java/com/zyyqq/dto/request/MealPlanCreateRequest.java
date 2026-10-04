package com.zyyqq.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 创建膳食方案的表单（对应「创建个性化膳食方案」弹窗）。
 */
@Data
public class MealPlanCreateRequest {

    /** 膳食目标：lose 减脂 / gain 增重 / maintain 维持体重 */
    private String goal;

    /** 方案周期（天）：3 / 7 / 14 */
    private Integer days;

    /** 每日餐次：breakfast / lunch / dinner / snack */
    private List<String> meals;

    /** 饮食偏好标签 */
    private List<String> preferences;

    /** 不喜欢 / 不能吃的食物 */
    private List<String> dislikes;

    /** 其他要求 */
    private String extraRequirement;
}
