package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 生成出来的方案草稿（尚未落库）。
 *
 * <p>除方案内容外，把创建表单的选择原样带上（目标/周期/餐次/偏好/忌口/其他要求），
 * 前端点「采用此方案」时直接回传即可，不用自己存一份表单状态。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanDraftVO {

    private String name;
    private String summary;
    private String goal;
    private String goalLabel;
    private Integer days;
    private List<String> meals;
    private BigDecimal dailyCalories;
    private List<String> preferences;
    private List<String> dislikes;
    private String extraRequirement;

    private List<MealPlanDayVO> planDays;

    /** 食物库里找不到、未计入营养统计的食物名（提示用户，不静默丢弃） */
    private List<String> unmatchedFoods;
}
