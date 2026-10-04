package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 替换候选列表。
 *
 * <p>{@code aiUsed=false} 表示 AI 没能参与排序（服务未启动 / 超时），
 * 列表退化成「按热量接近度」的算法结果。前端会据此明确提示用户，
 * 不让降级结果冒充成 AI 推荐。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanAlternativesVO {

    /** 被替换的原食物名 */
    private String originalFoodName;
    private boolean aiUsed;
    private String note;
    private List<FoodAlternativeVO> candidates;
}
