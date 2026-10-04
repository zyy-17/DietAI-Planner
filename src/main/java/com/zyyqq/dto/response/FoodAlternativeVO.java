package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 「AI 智能替换」的一个候选项：替换成什么、多少克、以及推荐理由。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodAlternativeVO {

    private Long foodId;
    private String foodSource;
    private String foodName;
    /** 建议克数（沿用原条目的克数，或模型建议的克数） */
    private BigDecimal amount;
    private String amountText;
    private BigDecimal calories;
    private BigDecimal protein;
    private BigDecimal carbohydrate;
    private BigDecimal fat;
    /** 推荐理由；AI 不可用时为空字符串 */
    private String reason;
}
