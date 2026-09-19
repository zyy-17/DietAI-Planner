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
public class FoodRecommendationResponse {

    private BigDecimal remainingCalories;
    private BigDecimal proteinGap;
    private String dietGoal;
    private List<String> candidateFoods;
    private List<ScoredFood> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoredFood {
        private Long foodId;
        private String foodName;
        private BigDecimal calories;
        private BigDecimal protein;
        private BigDecimal carbohydrate;
        private BigDecimal fat;
        private BigDecimal score;
    }
}