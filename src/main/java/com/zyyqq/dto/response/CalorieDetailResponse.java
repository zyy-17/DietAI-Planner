package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalorieDetailResponse {

    private List<DailyCalorie> dailyCalories;
    private List<MealCalorie> mealDistribution;
    private CalorieSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyCalorie {
        private String date;
        private BigDecimal calories;
        private BigDecimal targetCalories;
        private BigDecimal deviation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MealCalorie {
        private String mealType;
        private BigDecimal calories;
        private BigDecimal ratio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalorieSummary {
        private BigDecimal avgCalories;
        private BigDecimal targetCalories;
        private BigDecimal avgDeviation;
        private BigDecimal hitRate;
        private String overallStatus;
    }
}