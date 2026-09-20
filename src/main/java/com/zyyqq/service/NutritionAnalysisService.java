package com.zyyqq.service;

import com.zyyqq.dto.response.*;
import com.zyyqq.entity.DietRecord;
import com.zyyqq.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NutritionAnalysisService {

    private static final BigDecimal PROTEIN_CAL = new BigDecimal("4");
    private static final BigDecimal CARB_CAL = new BigDecimal("4");
    private static final BigDecimal FAT_CAL = new BigDecimal("9");

    private final DietRecordService dietRecordService;
    private final UserService userService;

    public NutritionAnalysisResponse analyze(Long userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<DietRecord> records = dietRecordService.getRecordsByDateRange(userId, startDate, endDate);

        Map<LocalDate, DailyAgg> dailyMap = buildDailyAggMap(records, startDate, endDate);
        List<NutritionAnalysisResponse.DailyNutrition> dailyData = new ArrayList<>();
        dailyMap.forEach((date, agg) -> dailyData.add(NutritionAnalysisResponse.DailyNutrition.builder()
                .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .calories(agg.calories).protein(agg.protein)
                .carbohydrate(agg.carbohydrate).fat(agg.fat).build()));

        long daysWithData = dailyMap.values().stream().filter(d -> d.calories.compareTo(BigDecimal.ZERO) > 0).count();
        if (daysWithData == 0) daysWithData = 1;

        AvgNutrition avg = calcAvgNutrition(dailyMap, daysWithData);
        MacroRatios ratios = calcMacroRatios(avg.protein, avg.carbohydrate, avg.fat);

        User user = userService.getUserById(userId);
        BigDecimal targetCalories = userService.calculateTargetCalories(user);
        String aiInterpretation = generateInterpretation(avg.calories, targetCalories, ratios.protein, ratios.carb, ratios.fat);

        return NutritionAnalysisResponse.builder()
                .dailyData(dailyData)
                .avgCalories(avg.calories).avgProtein(avg.protein)
                .avgCarbohydrate(avg.carbohydrate).avgFat(avg.fat)
                .proteinRatio(ratios.protein.setScale(1, RoundingMode.HALF_UP))
                .carbRatio(ratios.carb.setScale(1, RoundingMode.HALF_UP))
                .fatRatio(ratios.fat.setScale(1, RoundingMode.HALF_UP))
                .aiInterpretation(aiInterpretation).build();
    }

    public NutrientDetailResponse analyzeNutrients(Long userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<DietRecord> records = dietRecordService.getRecordsByDateRange(userId, startDate, endDate);
        User user = userService.getUserById(userId);

        Map<LocalDate, DailyAgg> dailyMap = buildDailyAggMap(records, startDate, endDate);
        List<NutrientDetailResponse.DailyNutrient> dailyNutrients = new ArrayList<>();
        dailyMap.forEach((date, agg) -> dailyNutrients.add(NutrientDetailResponse.DailyNutrient.builder()
                .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .protein(agg.protein).carbohydrate(agg.carbohydrate).fat(agg.fat)
                .fiber(BigDecimal.ZERO).water(BigDecimal.ZERO).build()));

        long daysWithData = dailyMap.values().stream().filter(d -> d.protein.add(d.carbohydrate).add(d.fat).compareTo(BigDecimal.ZERO) > 0).count();
        if (daysWithData == 0) daysWithData = 1;

        AvgNutrition avg = calcAvgNutrition(dailyMap, daysWithData);
        MacroRatios ratios = calcMacroRatios(avg.protein, avg.carbohydrate, avg.fat);

        BigDecimal targetProtein = resolveTarget(user.getTargetProtein(), new BigDecimal("65"));
        BigDecimal targetCarb = resolveTarget(user.getTargetCarbohydrate(), new BigDecimal("250"));
        BigDecimal targetFat = resolveTarget(user.getTargetFat(), new BigDecimal("55"));

        List<NutrientDetailResponse.NutrientAdvice> advices = List.of(
                buildNutrientAdvice("蛋白质", avg.protein, targetProtein, "建议增加鸡胸肉、鱼类、豆制品等高蛋白食物", "蛋白质摄入偏高，可适当减少肉类", "蛋白质摄入合理，继续保持"),
                buildNutrientAdvice("碳水化合物", avg.carbohydrate, targetCarb, "建议适当增加全谷物主食", "碳水偏高，建议减少精制主食", "碳水摄入合理"),
                buildNutrientAdvice("脂肪", avg.fat, targetFat, "可适当增加坚果、牛油果等健康脂肪", "建议减少油炸食品和高脂零食", "脂肪摄入在合理范围内")
        );

        return NutrientDetailResponse.builder()
                .dailyNutrients(dailyNutrients)
                .summary(NutrientDetailResponse.NutrientSummary.builder()
                        .avgProtein(avg.protein).avgCarbohydrate(avg.carbohydrate).avgFat(avg.fat)
                        .proteinRatio(ratios.protein).carbRatio(ratios.carb).fatRatio(ratios.fat)
                        .targetProtein(targetProtein).targetCarb(targetCarb).targetFat(targetFat).build())
                .advices(advices).build();
    }

    public CalorieDetailResponse analyzeCalories(Long userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<DietRecord> records = dietRecordService.getRecordsByDateRange(userId, startDate, endDate);
        User user = userService.getUserById(userId);
        BigDecimal targetCal = userService.calculateTargetCalories(user);

        Map<LocalDate, BigDecimal> dailyCalMap = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyCalMap.put(date, BigDecimal.ZERO);
        }
        for (DietRecord record : records) {
            dailyCalMap.merge(record.getRecordDate(), record.getCalories(), BigDecimal::add);
        }

        List<CalorieDetailResponse.DailyCalorie> dailyCalories = new ArrayList<>();
        int hitDays = 0;
        BigDecimal totalDeviation = BigDecimal.ZERO;
        for (Map.Entry<LocalDate, BigDecimal> entry : dailyCalMap.entrySet()) {
            BigDecimal deviation = entry.getValue().subtract(targetCal);
            dailyCalories.add(CalorieDetailResponse.DailyCalorie.builder()
                    .date(entry.getKey().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .calories(entry.getValue()).targetCalories(targetCal).deviation(deviation).build());
            if (deviation.abs().compareTo(targetCal.multiply(new BigDecimal("0.1"))) <= 0) hitDays++;
            totalDeviation = totalDeviation.add(deviation);
        }

        Map<String, BigDecimal> mealCalMap = new LinkedHashMap<>();
        mealCalMap.put("早餐", BigDecimal.ZERO);
        mealCalMap.put("午餐", BigDecimal.ZERO);
        mealCalMap.put("晚餐", BigDecimal.ZERO);
        mealCalMap.put("加餐", BigDecimal.ZERO);
        for (DietRecord record : records) {
            String mealKey = switch (record.getMealType()) {
                case "breakfast" -> "早餐";
                case "lunch" -> "午餐";
                case "dinner" -> "晚餐";
                default -> "加餐";
            };
            mealCalMap.merge(mealKey, record.getCalories(), BigDecimal::add);
        }
        BigDecimal totalMealCal = mealCalMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CalorieDetailResponse.MealCalorie> mealDistribution = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : mealCalMap.entrySet()) {
            BigDecimal ratio = totalMealCal.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().multiply(new BigDecimal("100")).divide(totalMealCal, 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            mealDistribution.add(CalorieDetailResponse.MealCalorie.builder().mealType(entry.getKey()).calories(entry.getValue()).ratio(ratio).build());
        }

        long daysWithData = dailyCalMap.values().stream().filter(c -> c.compareTo(BigDecimal.ZERO) > 0).count();
        if (daysWithData == 0) daysWithData = 1;
        BigDecimal avgCalories = dailyCalMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(daysWithData), 0, RoundingMode.HALF_UP);
        BigDecimal avgDeviation = totalDeviation.divide(BigDecimal.valueOf(daysWithData), 0, RoundingMode.HALF_UP);
        BigDecimal hitRate = BigDecimal.valueOf(hitDays).multiply(new BigDecimal("100")).divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP);

        String overallStatus;
        if (hitRate.compareTo(new BigDecimal("80")) >= 0) overallStatus = "良好";
        else if (hitRate.compareTo(new BigDecimal("50")) >= 0) overallStatus = "一般";
        else overallStatus = "较差";

        return CalorieDetailResponse.builder()
                .dailyCalories(dailyCalories).mealDistribution(mealDistribution)
                .summary(CalorieDetailResponse.CalorieSummary.builder()
                        .avgCalories(avgCalories).targetCalories(targetCal)
                        .avgDeviation(avgDeviation).hitRate(hitRate).overallStatus(overallStatus).build())
                .build();
    }

    public GoalCompletionResponse analyzeGoalCompletion(Long userId) {
        User user = userService.getUserById(userId);
        List<DietRecord> todayRecords = dietRecordService.getTodayRecords(userId);

        BigDecimal actualCal = todayRecords.stream().map(DietRecord::getCalories).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualProtein = sumNutrient(todayRecords, DietRecord::getProtein);
        BigDecimal actualCarb = sumNutrient(todayRecords, DietRecord::getCarbohydrate);
        BigDecimal actualFat = sumNutrient(todayRecords, DietRecord::getFat);

        BigDecimal targetCal = userService.calculateTargetCalories(user);
        BigDecimal targetProtein = resolveTarget(user.getTargetProtein(), new BigDecimal("65"));
        BigDecimal targetCarb = resolveTarget(user.getTargetCarbohydrate(), new BigDecimal("250"));
        BigDecimal targetFat = resolveTarget(user.getTargetFat(), new BigDecimal("55"));

        List<GoalCompletionResponse.GoalItem> goals = List.of(
                buildGoalItem("热量", targetCal, actualCal),
                buildGoalItem("蛋白质", targetProtein, actualProtein),
                buildGoalItem("碳水化合物", targetCarb, actualCarb),
                buildGoalItem("脂肪", targetFat, actualFat)
        );

        BigDecimal overallCompletion = goals.stream().map(GoalCompletionResponse.GoalItem::getCompletion)
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(goals.size()), 0, RoundingMode.HALF_UP);

        String overallStatus;
        if (overallCompletion.compareTo(new BigDecimal("90")) >= 0) overallStatus = "优秀";
        else if (overallCompletion.compareTo(new BigDecimal("70")) >= 0) overallStatus = "良好";
        else if (overallCompletion.compareTo(new BigDecimal("50")) >= 0) overallStatus = "一般";
        else overallStatus = "不足";

        List<String> suggestions = new ArrayList<>();
        for (GoalCompletionResponse.GoalItem goal : goals) {
            if ("不足".equals(goal.getStatus())) {
                suggestions.add(goal.getName() + "还差" + goal.getGap().setScale(0, RoundingMode.HALF_UP) + "，建议增加摄入");
            } else if ("超标".equals(goal.getStatus())) {
                suggestions.add(goal.getName() + "超出" + goal.getGap().abs().setScale(0, RoundingMode.HALF_UP) + "，建议控制摄入");
            }
        }
        if (suggestions.isEmpty()) suggestions.add("各营养素摄入均衡，继续保持！");

        return GoalCompletionResponse.builder()
                .goals(goals).overallCompletion(overallCompletion)
                .overallStatus(overallStatus).suggestions(suggestions).build();
    }

    public NutritionReportResponse generateReport(Long userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<DietRecord> records = dietRecordService.getRecordsByDateRange(userId, startDate, endDate);
        User user = userService.getUserById(userId);

        long daysWithData = records.stream().map(DietRecord::getRecordDate).distinct().count();
        if (daysWithData == 0) daysWithData = 1;

        BigDecimal totalCal = records.stream().map(DietRecord::getCalories).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProtein = sumNutrient(records, DietRecord::getProtein);
        BigDecimal totalCarb = sumNutrient(records, DietRecord::getCarbohydrate);
        BigDecimal totalFat = sumNutrient(records, DietRecord::getFat);

        BigDecimal avgCalories = totalCal.divide(BigDecimal.valueOf(daysWithData), 0, RoundingMode.HALF_UP);
        BigDecimal avgProtein = totalProtein.divide(BigDecimal.valueOf(daysWithData), 1, RoundingMode.HALF_UP);
        BigDecimal avgCarb = totalCarb.divide(BigDecimal.valueOf(daysWithData), 1, RoundingMode.HALF_UP);
        BigDecimal avgFat = totalFat.divide(BigDecimal.valueOf(daysWithData), 1, RoundingMode.HALF_UP);

        MacroRatios ratios = calcMacroRatios(avgProtein, avgCarb, avgFat);

        BigDecimal targetCal = userService.calculateTargetCalories(user);
        BigDecimal targetProtein = resolveTarget(user.getTargetProtein(), new BigDecimal("65"));
        BigDecimal targetCarb = resolveTarget(user.getTargetCarbohydrate(), new BigDecimal("250"));
        BigDecimal targetFat = resolveTarget(user.getTargetFat(), new BigDecimal("55"));

        int calorieScore = calculateItemScore(avgCalories, targetCal);
        int proteinScore = calculateItemScore(avgProtein, targetProtein);
        int carbScore = calculateItemScore(avgCarb, targetCarb);
        int fatScore = calculateItemScore(avgFat, targetFat);
        int nutritionScore = (calorieScore * 30 + proteinScore * 30 + carbScore * 20 + fatScore * 20) / 100;

        List<String> keyFindings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (calorieScore >= 80) keyFindings.add("热量摄入基本达标");
        else if (calorieScore >= 50) keyFindings.add("热量摄入略有偏差");
        else keyFindings.add("热量摄入严重偏离目标");

        if (proteinScore < 70) { keyFindings.add("蛋白质摄入不足"); recommendations.add("增加优质蛋白：鸡胸肉、鱼类、鸡蛋、豆制品"); }
        if (carbScore < 70) { keyFindings.add("碳水化合物摄入不足"); recommendations.add("适当增加全谷物、燕麦、红薯等主食"); }
        if (fatScore < 70) { keyFindings.add("脂肪摄入不足"); recommendations.add("适量补充坚果、牛油果等健康脂肪"); }
        if (ratios.fat.compareTo(new BigDecimal("35")) > 0) { keyFindings.add("脂肪供能比例偏高"); recommendations.add("减少油炸食品，控制烹饪用油"); }
        if (ratios.protein.compareTo(new BigDecimal("15")) < 0) { keyFindings.add("蛋白质供能比例偏低"); recommendations.add("每餐保证一份优质蛋白来源"); }

        if (recommendations.isEmpty()) recommendations.add("当前饮食结构合理，继续保持均衡饮食");

        return NutritionReportResponse.builder()
                .period(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + " ~ " + endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .days(days).avgCalories(avgCalories).avgProtein(avgProtein).avgCarbohydrate(avgCarb).avgFat(avgFat)
                .proteinRatio(ratios.protein).carbRatio(ratios.carb).fatRatio(ratios.fat)
                .nutritionScore(BigDecimal.valueOf(nutritionScore))
                .calorieEvaluation(evaluateLevel(calorieScore)).proteinEvaluation(evaluateLevel(proteinScore))
                .carbEvaluation(evaluateLevel(carbScore)).fatEvaluation(evaluateLevel(fatScore))
                .keyFindings(keyFindings).recommendations(recommendations).build();
    }

    private Map<LocalDate, DailyAgg> buildDailyAggMap(List<DietRecord> records, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, DailyAgg> dailyMap = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyMap.put(date, new DailyAgg());
        }
        for (DietRecord record : records) {
            DailyAgg agg = dailyMap.get(record.getRecordDate());
            if (agg != null) {
                agg.calories = agg.calories.add(record.getCalories());
                agg.protein = agg.protein.add(nullToZero(record.getProtein()));
                agg.carbohydrate = agg.carbohydrate.add(nullToZero(record.getCarbohydrate()));
                agg.fat = agg.fat.add(nullToZero(record.getFat()));
            }
        }
        return dailyMap;
    }

    private AvgNutrition calcAvgNutrition(Map<LocalDate, DailyAgg> dailyMap, long daysWithData) {
        BigDecimal totalCal = dailyMap.values().stream().map(d -> d.calories).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProtein = dailyMap.values().stream().map(d -> d.protein).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCarb = dailyMap.values().stream().map(d -> d.carbohydrate).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFat = dailyMap.values().stream().map(d -> d.fat).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal d = BigDecimal.valueOf(daysWithData);
        return new AvgNutrition(
                totalCal.divide(d, 2, RoundingMode.HALF_UP),
                totalProtein.divide(d, 2, RoundingMode.HALF_UP),
                totalCarb.divide(d, 2, RoundingMode.HALF_UP),
                totalFat.divide(d, 2, RoundingMode.HALF_UP)
        );
    }

    private MacroRatios calcMacroRatios(BigDecimal avgProtein, BigDecimal avgCarb, BigDecimal avgFat) {
        BigDecimal totalCalFromMacros = avgProtein.multiply(PROTEIN_CAL).add(avgCarb.multiply(CARB_CAL)).add(avgFat.multiply(FAT_CAL));
        if (totalCalFromMacros.compareTo(BigDecimal.ZERO) <= 0) {
            return new MacroRatios(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal p = avgProtein.multiply(PROTEIN_CAL).multiply(new BigDecimal("100")).divide(totalCalFromMacros, 1, RoundingMode.HALF_UP);
        BigDecimal c = avgCarb.multiply(CARB_CAL).multiply(new BigDecimal("100")).divide(totalCalFromMacros, 1, RoundingMode.HALF_UP);
        BigDecimal f = avgFat.multiply(FAT_CAL).multiply(new BigDecimal("100")).divide(totalCalFromMacros, 1, RoundingMode.HALF_UP);
        return new MacroRatios(p, c, f);
    }

    private BigDecimal sumNutrient(List<DietRecord> records, java.util.function.Function<DietRecord, BigDecimal> extractor) {
        return records.stream().map(r -> nullToZero(extractor.apply(r))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nullToZero(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private BigDecimal resolveTarget(BigDecimal userTarget, BigDecimal fallback) {
        return userTarget != null ? userTarget : fallback;
    }

    private String generateInterpretation(BigDecimal avgCalories, BigDecimal targetCalories,
                                           BigDecimal proteinRatio, BigDecimal carbRatio, BigDecimal fatRatio) {
        StringBuilder sb = new StringBuilder();
        if (targetCalories.compareTo(BigDecimal.ZERO) == 0) {
            return "请先完善您的个人健康信息（身高、体重、年龄等），以便获得个性化营养建议。";
        }
        BigDecimal diff = avgCalories.subtract(targetCalories);
        if (diff.abs().compareTo(targetCalories.multiply(new BigDecimal("0.1"))) <= 0) {
            sb.append("您近期的平均热量摄入基本达标，");
        } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("您近期的平均热量摄入偏高，超出目标约").append(diff.setScale(0, RoundingMode.HALF_UP)).append("千卡，");
        } else {
            sb.append("您近期的平均热量摄入偏低，低于目标约").append(diff.abs().setScale(0, RoundingMode.HALF_UP)).append("千卡，");
        }
        if (proteinRatio.compareTo(new BigDecimal("15")) < 0) sb.append("蛋白质摄入偏低，建议增加鸡胸肉、鱼类、豆制品等高蛋白食物；");
        else if (proteinRatio.compareTo(new BigDecimal("25")) > 0) sb.append("蛋白质摄入偏高，可适当减少肉类摄入；");
        else sb.append("蛋白质摄入合理；");
        if (carbRatio.compareTo(new BigDecimal("45")) < 0) sb.append("碳水化合物摄入偏低，建议适当增加全谷物主食；");
        else if (carbRatio.compareTo(new BigDecimal("60")) > 0) sb.append("碳水化合物摄入偏高，建议减少精制主食，增加蔬菜比例；");
        if (fatRatio.compareTo(new BigDecimal("25")) < 0) sb.append("脂肪摄入偏低，可适当增加坚果、牛油果等健康脂肪来源。");
        else if (fatRatio.compareTo(new BigDecimal("35")) > 0) sb.append("脂肪摄入偏高，建议减少油炸食品和高脂零食。");
        else sb.append("脂肪摄入在合理范围内。");
        return sb.toString();
    }

    private GoalCompletionResponse.GoalItem buildGoalItem(String name, BigDecimal target, BigDecimal actual) {
        BigDecimal completion = target.compareTo(BigDecimal.ZERO) > 0
                ? actual.multiply(new BigDecimal("100")).divide(target, 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        if (completion.compareTo(new BigDecimal("100")) > 0) completion = new BigDecimal("100");
        BigDecimal gap = target.subtract(actual);
        String status;
        if (gap.compareTo(BigDecimal.ZERO) < 0) status = "超标";
        else if (completion.compareTo(new BigDecimal("90")) >= 0) status = "达标";
        else if (completion.compareTo(new BigDecimal("70")) >= 0) status = "接近";
        else if (completion.compareTo(new BigDecimal("30")) >= 0) status = "不足";
        else status = "严重不足";
        return GoalCompletionResponse.GoalItem.builder().name(name).target(target).actual(actual).completion(completion).status(status).gap(gap).build();
    }

    private NutrientDetailResponse.NutrientAdvice buildNutrientAdvice(String nutrient, BigDecimal avg, BigDecimal target,
                                                                        String lowSuggestion, String highSuggestion, String normalSuggestion) {
        String status, suggestion;
        if (avg.compareTo(target) < 0) { status = "不足"; suggestion = lowSuggestion; }
        else if (avg.compareTo(target.multiply(new BigDecimal("1.2"))) > 0) { status = "偏高"; suggestion = highSuggestion; }
        else { status = "正常"; suggestion = normalSuggestion; }
        return NutrientDetailResponse.NutrientAdvice.builder().nutrient(nutrient).status(status).suggestion(suggestion).build();
    }

    private int calculateItemScore(BigDecimal actual, BigDecimal target) {
        if (target.compareTo(BigDecimal.ZERO) == 0) return 0;
        BigDecimal ratio = actual.divide(target, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.9")) >= 0 && ratio.compareTo(new BigDecimal("1.1")) <= 0) return 100;
        if (ratio.compareTo(new BigDecimal("0.8")) >= 0 && ratio.compareTo(new BigDecimal("1.2")) <= 0) return 80;
        if (ratio.compareTo(new BigDecimal("0.7")) >= 0 && ratio.compareTo(new BigDecimal("1.3")) <= 0) return 60;
        if (ratio.compareTo(new BigDecimal("0.5")) >= 0 && ratio.compareTo(new BigDecimal("1.5")) <= 0) return 40;
        return 20;
    }

    private String evaluateLevel(int score) {
        if (score >= 90) return "优秀";
        if (score >= 70) return "良好";
        if (score >= 50) return "一般";
        return "较差";
    }

    private static class DailyAgg {
        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carbohydrate = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
    }

    private record AvgNutrition(BigDecimal calories, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat) {}
    private record MacroRatios(BigDecimal protein, BigDecimal carb, BigDecimal fat) {}
}