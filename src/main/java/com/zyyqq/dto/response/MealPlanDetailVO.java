package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 方案详情页：方案概要 + 逐天明细。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanDetailVO {

    private MealPlanVO plan;
    private List<MealPlanDayVO> planDays;
}
