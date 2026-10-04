package com.zyyqq.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 替换方案里某一条食物。
 *
 * <p>按优先级识别新食物：{@code foodId + foodSource} 命中食物库 → 按 {@code foodName} 模糊匹配。
 * 两者都没命中时抛业务异常，不做静默占位——否则方案里会出现一条没有营养数据的幽灵食物。</p>
 */
@Data
public class MealPlanItemReplaceRequest {

    private Long foodId;

    /** 食物来源：system 食物库 / user 自定义食物 / ai（不传 foodId 时按名称匹配） */
    private String foodSource;

    private String foodName;

    /** 克数；为空时沿用原条目的克数 */
    private BigDecimal amount;
}
