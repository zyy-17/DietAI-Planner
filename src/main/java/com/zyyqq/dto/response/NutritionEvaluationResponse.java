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
public class NutritionEvaluationResponse {

    private BigDecimal bmi;
    private BigDecimal bmr;
    private BigDecimal tdee;
    private BigDecimal targetCalories;
    private String calorieStatus;
    private String proteinStatus;
    private String carbStatus;
    private String fatStatus;
    private int nutritionScore;
    private String mainProblem;
    private BigDecimal calorieGap;
    private BigDecimal proteinGap;
    private BigDecimal carbGap;
    private BigDecimal fatGap;
    private BigDecimal actualCalories;
    private BigDecimal actualProtein;
    private BigDecimal actualCarb;
    private BigDecimal actualFat;
}