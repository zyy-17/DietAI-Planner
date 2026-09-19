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
public class NutritionReportResponse {

    private String period;
    private int days;
    private BigDecimal avgCalories;
    private BigDecimal avgProtein;
    private BigDecimal avgCarbohydrate;
    private BigDecimal avgFat;
    private BigDecimal proteinRatio;
    private BigDecimal carbRatio;
    private BigDecimal fatRatio;
    private BigDecimal nutritionScore;
    private String calorieEvaluation;
    private String proteinEvaluation;
    private String carbEvaluation;
    private String fatEvaluation;
    private List<String> keyFindings;
    private List<String> recommendations;
}