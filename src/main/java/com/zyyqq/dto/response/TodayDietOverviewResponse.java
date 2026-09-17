package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayDietOverviewResponse {

    private BigDecimal totalCalories;
    private BigDecimal targetCalories;
    private BigDecimal remainingCalories;
    private BigDecimal totalProtein;
    private BigDecimal totalCarbohydrate;
    private BigDecimal totalFat;
    private BigDecimal targetProtein;
    private BigDecimal targetCarbohydrate;
    private BigDecimal targetFat;
}