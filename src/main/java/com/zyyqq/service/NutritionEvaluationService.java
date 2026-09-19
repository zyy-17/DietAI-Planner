package com.zyyqq.service;

import com.zyyqq.entity.DietRecord;
import com.zyyqq.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NutritionEvaluationService {

    private final UserService userService;
    private final DietRecordService dietRecordService;

    private static final BigDecimal PROTEIN_CAL_FACTOR = new BigDecimal("4");
    private static final BigDecimal CARB_CAL_FACTOR = new BigDecimal("4");
    private static final BigDecimal FAT_CAL_FACTOR = new BigDecimal("9");

    /** 计算BMI（体重kg / 身高m²） */
    public BigDecimal calculateBMI(User user) {
        if (user.getWeight() == null || user.getHeight() == null) return BigDecimal.ZERO;
        if (user.getHeight().compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        BigDecimal heightM = user.getHeight().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        if (heightM.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return user.getWeight().divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }

    /** 计算BMR（委托给UserService的Mifflin-St Jeor公式） */
    public BigDecimal calculateBMR(User user) {
        return userService.calculateBMR(user);
    }

    /** 计算TDEE（委托给UserService） */
    public BigDecimal calculateTDEE(User user) {
        return userService.calculateTDEE(user);
    }

    /** 计算目标热量（委托给UserService） */
    public BigDecimal calculateTargetCalories(User user) {
        return userService.calculateTargetCalories(user);
    }

    /** 计算今日营养缺口（目标 - 实际摄入） */
    public NutritionGap calculateNutritionGap(Long userId) {
        User user = userService.getUserById(userId);
        BigDecimal targetCal = calculateTargetCalories(user);
        BigDecimal targetProtein = targetCal.multiply(new BigDecimal("0.20")).divide(PROTEIN_CAL_FACTOR, 1, RoundingMode.HALF_UP);
        BigDecimal targetCarb = targetCal.multiply(new BigDecimal("0.50")).divide(CARB_CAL_FACTOR, 1, RoundingMode.HALF_UP);
        BigDecimal targetFat = targetCal.multiply(new BigDecimal("0.30")).divide(FAT_CAL_FACTOR, 1, RoundingMode.HALF_UP);

        List<DietRecord> todayRecords = dietRecordService.getTodayRecords(userId);
        BigDecimal actualCal = todayRecords.stream().map(DietRecord::getCalories).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualProtein = todayRecords.stream().map(r -> r.getProtein() != null ? r.getProtein() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualCarb = todayRecords.stream().map(r -> r.getCarbohydrate() != null ? r.getCarbohydrate() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualFat = todayRecords.stream().map(r -> r.getFat() != null ? r.getFat() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

        return NutritionGap.builder()
                .calorieGap(targetCal.subtract(actualCal))
                .proteinGap(targetProtein.subtract(actualProtein))
                .carbGap(targetCarb.subtract(actualCarb))
                .fatGap(targetFat.subtract(actualFat))
                .targetCalories(targetCal)
                .targetProtein(targetProtein)
                .targetCarb(targetCarb)
                .targetFat(targetFat)
                .actualCalories(actualCal)
                .actualProtein(actualProtein)
                .actualCarb(actualCarb)
                .actualFat(actualFat)
                .build();
    }

    /** 综合评估营养状态，返回评估结果和评分 */
    public NutritionEvaluationResult evaluateNutritionStatus(Long userId) {
        User user = userService.getUserById(userId);
        NutritionGap gap = calculateNutritionGap(userId);

        BigDecimal bmi = calculateBMI(user);
        BigDecimal bmr = calculateBMR(user);
        BigDecimal tdee = calculateTDEE(user);

        String calorieStatus = evaluateStatus(gap.getCalorieGap(), gap.getTargetCalories());
        String proteinStatus = evaluateStatus(gap.getProteinGap(), gap.getTargetProtein());
        String carbStatus = evaluateStatus(gap.getCarbGap(), gap.getTargetCarb());
        String fatStatus = evaluateStatus(gap.getFatGap(), gap.getTargetFat());

        int nutritionScore = calculateNutritionScore(gap);

        String mainProblem = identifyMainProblem(calorieStatus, proteinStatus, carbStatus, fatStatus);

        return NutritionEvaluationResult.builder()
                .bmi(bmi)
                .bmr(bmr)
                .tdee(tdee)
                .targetCalories(gap.getTargetCalories())
                .calorieStatus(calorieStatus)
                .proteinStatus(proteinStatus)
                .carbStatus(carbStatus)
                .fatStatus(fatStatus)
                .nutritionScore(nutritionScore)
                .mainProblem(mainProblem)
                .calorieGap(gap.getCalorieGap())
                .proteinGap(gap.getProteinGap())
                .carbGap(gap.getCarbGap())
                .fatGap(gap.getFatGap())
                .actualCalories(gap.getActualCalories())
                .actualProtein(gap.getActualProtein())
                .actualCarb(gap.getActualCarb())
                .actualFat(gap.getActualFat())
                .build();
    }

    /** 评估单项营养素状态：不足/正常/偏高 */
    private String evaluateStatus(BigDecimal gap, BigDecimal target) {
        if (target.compareTo(BigDecimal.ZERO) == 0) return "正常";
        BigDecimal ratio = gap.divide(target, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.15")) > 0) return "不足";
        if (ratio.compareTo(new BigDecimal("-0.10")) < 0) return "偏高";
        return "正常";
    }

    /** 计算营养评分（热量30+蛋白质30+碳水20+脂肪20=100分） */
    private int calculateNutritionScore(NutritionGap gap) {
        int calorieScore = scoreItem(gap.getCalorieGap(), gap.getTargetCalories());
        int proteinScore = scoreItem(gap.getProteinGap(), gap.getTargetProtein());
        int carbScore = scoreItem(gap.getCarbGap(), gap.getTargetCarb());
        int fatScore = scoreItem(gap.getFatGap(), gap.getTargetFat());

        return (calorieScore * 30 + proteinScore * 30 + carbScore * 20 + fatScore * 20) / 100;
    }

    /** 单项评分：根据缺口比例打0-100分 */
    private int scoreItem(BigDecimal gap, BigDecimal target) {
        if (target.compareTo(BigDecimal.ZERO) == 0) return 100;
        BigDecimal ratio = gap.abs().divide(target, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.05")) <= 0) return 100;
        if (ratio.compareTo(new BigDecimal("0.10")) <= 0) return 90;
        if (ratio.compareTo(new BigDecimal("0.20")) <= 0) return 75;
        if (ratio.compareTo(new BigDecimal("0.30")) <= 0) return 60;
        if (ratio.compareTo(new BigDecimal("0.50")) <= 0) return 40;
        return 20;
    }

    /** 识别主要营养问题 */
    private String identifyMainProblem(String calorieStatus, String proteinStatus,
                                        String carbStatus, String fatStatus) {
        if ("不足".equals(proteinStatus)) return "蛋白质摄入不足";
        if ("不足".equals(calorieStatus)) return "热量摄入不足";
        if ("偏高".equals(fatStatus)) return "脂肪摄入偏高";
        if ("偏高".equals(calorieStatus)) return "热量摄入偏高";
        if ("不足".equals(carbStatus)) return "碳水化合物摄入不足";
        if ("偏高".equals(carbStatus)) return "碳水化合物摄入偏高";
        if ("不足".equals(fatStatus)) return "脂肪摄入不足";
        return "营养摄入均衡";
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NutritionGap {
        private BigDecimal calorieGap;
        private BigDecimal proteinGap;
        private BigDecimal carbGap;
        private BigDecimal fatGap;
        private BigDecimal targetCalories;
        private BigDecimal targetProtein;
        private BigDecimal targetCarb;
        private BigDecimal targetFat;
        private BigDecimal actualCalories;
        private BigDecimal actualProtein;
        private BigDecimal actualCarb;
        private BigDecimal actualFat;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NutritionEvaluationResult {
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
}