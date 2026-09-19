package com.zyyqq.controller;

import com.zyyqq.dto.response.*;
import com.zyyqq.service.NutritionAnalysisService;
import com.zyyqq.service.NutritionEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nutrition")
@RequiredArgsConstructor
public class NutritionAnalysisController {

    private final NutritionAnalysisService nutritionAnalysisService;
    private final NutritionEvaluationService nutritionEvaluationService;

    @GetMapping("/analysis")
    public ApiResponse<NutritionAnalysisResponse> analyze(
            Authentication authentication,
            @RequestParam(defaultValue = "7") int days) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(nutritionAnalysisService.analyze(userId, days));
    }

    /** 智能营养评估：BMI/BMR/TDEE/缺口/评分/状态 */
    @GetMapping("/evaluate")
    public ApiResponse<NutritionEvaluationResponse> evaluate(Authentication authentication) {
        Long userId = getUserId(authentication);
        NutritionEvaluationService.NutritionEvaluationResult result =
                nutritionEvaluationService.evaluateNutritionStatus(userId);
        NutritionEvaluationResponse response = NutritionEvaluationResponse.builder()
                .bmi(result.getBmi())
                .bmr(result.getBmr())
                .tdee(result.getTdee())
                .targetCalories(result.getTargetCalories())
                .calorieStatus(result.getCalorieStatus())
                .proteinStatus(result.getProteinStatus())
                .carbStatus(result.getCarbStatus())
                .fatStatus(result.getFatStatus())
                .nutritionScore(result.getNutritionScore())
                .mainProblem(result.getMainProblem())
                .calorieGap(result.getCalorieGap())
                .proteinGap(result.getProteinGap())
                .carbGap(result.getCarbGap())
                .fatGap(result.getFatGap())
                .actualCalories(result.getActualCalories())
                .actualProtein(result.getActualProtein())
                .actualCarb(result.getActualCarb())
                .actualFat(result.getActualFat())
                .build();
        return ApiResponse.success(response);
    }

    /** 营养素详细分析：各营养素每日趋势+比例+建议 */
    @GetMapping("/nutrients")
    public ApiResponse<NutrientDetailResponse> nutrients(
            Authentication authentication,
            @RequestParam(defaultValue = "7") int days) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(nutritionAnalysisService.analyzeNutrients(userId, days));
    }

    /** 热量详细分析：每日热量+时段分布+达标率 */
    @GetMapping("/calorie")
    public ApiResponse<CalorieDetailResponse> calorie(
            Authentication authentication,
            @RequestParam(defaultValue = "7") int days) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(nutritionAnalysisService.analyzeCalories(userId, days));
    }

    /** 目标完成度分析：各营养素完成百分比+建议 */
    @GetMapping("/goal")
    public ApiResponse<GoalCompletionResponse> goal(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(nutritionAnalysisService.analyzeGoalCompletion(userId));
    }

    /** 营养报告：综合分析+评分+关键发现+建议 */
    @GetMapping("/report")
    public ApiResponse<NutritionReportResponse> report(
            Authentication authentication,
            @RequestParam(defaultValue = "7") int days) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(nutritionAnalysisService.generateReport(userId, days));
    }

    /** 从认证信息中提取用户ID */
    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}