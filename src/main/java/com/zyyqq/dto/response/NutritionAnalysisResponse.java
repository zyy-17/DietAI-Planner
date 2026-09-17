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
public class NutritionAnalysisResponse {

    private List<DailyNutrition> dailyData;
    private BigDecimal avgCalories;
    private BigDecimal avgProtein;
    private BigDecimal avgCarbohydrate;
    private BigDecimal avgFat;
    private BigDecimal proteinRatio;
    private BigDecimal carbRatio;
    private BigDecimal fatRatio;
    private String aiInterpretation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyNutrition {
        private String date;
        private BigDecimal calories;
        private BigDecimal protein;
        private BigDecimal carbohydrate;
        private BigDecimal fat;
    }
}