package com.zyyqq.service;

import com.zyyqq.entity.Food;
import com.zyyqq.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodScoreCalculator {

    private static final BigDecimal CALORIE_WEIGHT = new BigDecimal("0.40");
    private static final BigDecimal PROTEIN_WEIGHT = new BigDecimal("0.30");
    private static final BigDecimal PREFERENCE_WEIGHT = new BigDecimal("0.15");
    private static final BigDecimal GOAL_WEIGHT = new BigDecimal("0.15");

    private static final java.util.Map<String, BigDecimal[]> MEAL_WEIGHTS = java.util.Map.of(
        "breakfast", new BigDecimal[]{new BigDecimal("0.30"), new BigDecimal("0.20"), new BigDecimal("0.15"), new BigDecimal("0.15")},
        "lunch",     new BigDecimal[]{new BigDecimal("0.35"), new BigDecimal("0.30"), new BigDecimal("0.15"), new BigDecimal("0.15")},
        "dinner",    new BigDecimal[]{new BigDecimal("0.30"), new BigDecimal("0.35"), new BigDecimal("0.15"), new BigDecimal("0.15")},
        "snack",     new BigDecimal[]{new BigDecimal("0.20"), new BigDecimal("0.40"), new BigDecimal("0.15"), new BigDecimal("0.15")}
    );

    /** 计算单个食物的综合推荐分数（0-100），支持餐次感知权重 */
    public FoodScoreResult calculateScore(Food food, RecommendationContext context) {
        BigDecimal calW, proW, prefW, goalW;
        if (context.getMealType() != null && MEAL_WEIGHTS.containsKey(context.getMealType())) {
            BigDecimal[] weights = MEAL_WEIGHTS.get(context.getMealType());
            calW = weights[0]; proW = weights[1]; prefW = weights[2]; goalW = weights[3];
        } else {
            calW = CALORIE_WEIGHT; proW = PROTEIN_WEIGHT; prefW = PREFERENCE_WEIGHT; goalW = GOAL_WEIGHT;
        }

        BigDecimal calorieScore = calculateCalorieMatchScore(food, context);
        BigDecimal proteinScore = calculateProteinMatchScore(food, context);
        BigDecimal preferenceScore = calculatePreferenceScore(food, context);
        BigDecimal goalScore = calculateGoalScore(food, context);

        BigDecimal totalScore = calorieScore.multiply(calW)
                .add(proteinScore.multiply(proW))
                .add(preferenceScore.multiply(prefW))
                .add(goalScore.multiply(goalW));

        return FoodScoreResult.builder()
                .foodId(food.getId())
                .foodName(food.getName())
                .calories(food.getCalories())
                .protein(food.getProtein())
                .carbohydrate(food.getCarbohydrate())
                .fat(food.getFat())
                .totalScore(totalScore.setScale(1, RoundingMode.HALF_UP))
                .calorieScore(calorieScore.setScale(1, RoundingMode.HALF_UP))
                .proteinScore(proteinScore.setScale(1, RoundingMode.HALF_UP))
                .preferenceScore(preferenceScore.setScale(1, RoundingMode.HALF_UP))
                .goalScore(goalScore.setScale(1, RoundingMode.HALF_UP))
                .build();
    }

    /** 批量计算食物推荐分数并排序 */
    public List<FoodScoreResult> calculateScores(List<Food> foods, RecommendationContext context) {
        return foods.stream()
                .map(food -> calculateScore(food, context))
                .sorted(Comparator.comparing(FoodScoreResult::getTotalScore).reversed())
                .collect(Collectors.toList());
    }

    /** 热量匹配度：食物热量与剩余热量的匹配程度 */
    private BigDecimal calculateCalorieMatchScore(Food food, RecommendationContext context) {
        if (context.getRemainingCalories().compareTo(BigDecimal.ZERO) <= 0) return new BigDecimal("50");
        BigDecimal foodCal = food.getCalories();
        BigDecimal remaining = context.getRemainingCalories();
        BigDecimal ratio = foodCal.divide(remaining, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.3")) >= 0 && ratio.compareTo(new BigDecimal("0.6")) <= 0) {
            return new BigDecimal("100");
        }
        if (ratio.compareTo(new BigDecimal("0.15")) >= 0 && ratio.compareTo(new BigDecimal("0.85")) <= 0) {
            return new BigDecimal("80");
        }
        if (ratio.compareTo(new BigDecimal("0.05")) >= 0 && ratio.compareTo(new BigDecimal("1.0")) <= 0) {
            return new BigDecimal("60");
        }
        return new BigDecimal("30");
    }

    /** 蛋白质匹配度：蛋白质缺口越大，高蛋白食物分数越高 */
    private BigDecimal calculateProteinMatchScore(Food food, RecommendationContext context) {
        if (context.getProteinGap().compareTo(BigDecimal.ZERO) <= 0) return new BigDecimal("60");
        if (food.getProtein() == null || food.getProtein().compareTo(BigDecimal.ZERO) == 0) return new BigDecimal("30");
        BigDecimal proteinPer100Cal = food.getProtein().divide(
                food.getCalories().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : food.getCalories(),
                4, RoundingMode.HALF_UP);
        if (proteinPer100Cal.compareTo(new BigDecimal("0.08")) >= 0) return new BigDecimal("100");
        if (proteinPer100Cal.compareTo(new BigDecimal("0.05")) >= 0) return new BigDecimal("80");
        if (proteinPer100Cal.compareTo(new BigDecimal("0.02")) >= 0) return new BigDecimal("60");
        return new BigDecimal("40");
    }

    /** 用户偏好匹配度：食物名称是否匹配用户饮食偏好 */
    private BigDecimal calculatePreferenceScore(Food food, RecommendationContext context) {
        if (context.getDietPreference() == null || context.getDietPreference().isEmpty()) {
            return new BigDecimal("70");
        }
        String preference = context.getDietPreference().toLowerCase();
        String foodName = food.getName().toLowerCase();
        if (preference.contains("素食") || preference.contains("vegan")) {
            if (isVegetarianFood(foodName)) return new BigDecimal("100");
            return new BigDecimal("20");
        }
        if (preference.contains("清淡")) {
            if (food.getFat() != null && food.getFat().compareTo(new BigDecimal("10")) < 0) return new BigDecimal("100");
            return new BigDecimal("50");
        }
        if (preference.contains("低糖") || preference.contains("low-sugar")) {
            if (food.getCarbohydrate() != null && food.getCarbohydrate().compareTo(new BigDecimal("15")) < 0) return new BigDecimal("100");
            return new BigDecimal("50");
        }
        return new BigDecimal("70");
    }

    /** 饮食目标匹配度：根据减脂/增肌/维持调整分数 */
    private BigDecimal calculateGoalScore(Food food, RecommendationContext context) {
        String goal = context.getDietGoal();
        if (goal == null) return new BigDecimal("70");
        switch (goal) {
            case "lose":
                if (food.getCalories().compareTo(new BigDecimal("150")) < 0) return new BigDecimal("100");
                if (food.getCalories().compareTo(new BigDecimal("300")) < 0) return new BigDecimal("80");
                return new BigDecimal("40");
            case "gain":
                if (food.getProtein() != null && food.getProtein().compareTo(new BigDecimal("15")) >= 0) return new BigDecimal("100");
                if (food.getProtein() != null && food.getProtein().compareTo(new BigDecimal("8")) >= 0) return new BigDecimal("80");
                return new BigDecimal("50");
            default:
                return new BigDecimal("70");
        }
    }

    /** 判断是否为素食食物 */
    private boolean isVegetarianFood(String foodName) {
        String[] meatKeywords = {"猪肉", "牛肉", "羊肉", "鸡肉", "鸭肉", "鱼", "虾", "蟹", "肉", "鸡胸", "鸡腿", "排骨"};
        for (String keyword : meatKeywords) {
            if (foodName.contains(keyword)) return false;
        }
        return true;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RecommendationContext {
        private BigDecimal remainingCalories;
        private BigDecimal proteinGap;
        private String dietGoal;
        private String dietPreference;
        private List<String> avoidFoods;
        private String mealType;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FoodScoreResult {
        private Long foodId;
        private String foodName;
        private BigDecimal calories;
        private BigDecimal protein;
        private BigDecimal carbohydrate;
        private BigDecimal fat;
        private BigDecimal totalScore;
        private BigDecimal calorieScore;
        private BigDecimal proteinScore;
        private BigDecimal preferenceScore;
        private BigDecimal goalScore;
    }
}