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
public class NutrientDetailResponse {

    private List<DailyNutrient> dailyNutrients;
    private NutrientSummary summary;
    private List<NutrientAdvice> advices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyNutrient {
        private String date;
        private BigDecimal protein;
        private BigDecimal carbohydrate;
        private BigDecimal fat;
        private BigDecimal fiber;
        private BigDecimal water;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutrientSummary {
        private BigDecimal avgProtein;
        private BigDecimal avgCarbohydrate;
        private BigDecimal avgFat;
        private BigDecimal proteinRatio;
        private BigDecimal carbRatio;
        private BigDecimal fatRatio;
        private BigDecimal targetProtein;
        private BigDecimal targetCarb;
        private BigDecimal targetFat;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutrientAdvice {
        private String nutrient;
        private String status;
        private String suggestion;
    }
}