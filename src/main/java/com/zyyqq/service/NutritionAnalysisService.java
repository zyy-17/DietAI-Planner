package com.zyyqq.service;

import com.zyyqq.dto.response.*;
import com.zyyqq.entity.AiGenerationLog;
import com.zyyqq.entity.DietRecord;
import com.zyyqq.entity.User;
import com.zyyqq.repository.AiGenerationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NutritionAnalysisService {

    private final DietRecordService dietRecordService;
    private final UserService userService;
    private final AiGenerationLogRepository aiGenerationLogRepository;

    /** 分析指定天数内的营养摄入，计算日均和三大营养素比例 */
    public NutritionAnalysisResponse analyze(Long userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<DietRecord> records = dietRecordService.getRecordsByDateRange(userId, startDate, endDate);

        // 初始化每日营养数据Map，确保无记录的日期也显示零值
        java.util.Map<LocalDate, NutritionAnalysisResponse.DailyNutrition> dailyMap = new java.util.LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyMap.put(date, NutritionAnalysisResponse.DailyNutrition.builder()
                    .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .calories(BigDecimal.ZERO)
                    .protein(BigDecimal.ZERO)
                    .carbohydrate(BigDecimal.ZERO)
                    .fat(BigDecimal.ZERO)
                    .build());
        }

        for (DietRecord record : records) {
            NutritionAnalysisResponse.DailyNutrition daily = dailyMap.get(record.getRecordDate());
            if (daily != null) {
                daily.setCalories(daily.getCalories().add(record.getCalories()));
                daily.setProtein(daily.getProtein().add(record.getProtein() != null ? record.getProtein() : BigDecimal.ZERO));
                daily.setCarbohydrate(daily.getCarbohydrate().add(record.getCarbohydrate() != null ? record.getCarbohydrate() : BigDecimal.ZERO));
                daily.setFat(daily.getFat().add(record.getFat() != null ? record.getFat() : BigDecimal.ZERO));
            }
        }

        List<NutritionAnalysisResponse.DailyNutrition> dailyData = new ArrayList<>(dailyMap.values());

        long daysWithData = dailyData.stream()
                .filter(d -> d.getCalories().compareTo(BigDecimal.ZERO) > 0)
                .count();

        if (daysWithData == 0) daysWithData = 1;

        BigDecimal avgCalories = dailyData.stream()
                .map(NutritionAnalysisResponse.DailyNutrition::getCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(daysWithData), 2, RoundingMode.HALF_UP);

        BigDecimal avgProtein = dailyData.stream()
                .map(NutritionAnalysisResponse.DailyNutrition::getProtein)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(daysWithData), 2, RoundingMode.HALF_UP);

        BigDecimal avgCarbohydrate = dailyData.stream()
                .map(NutritionAnalysisResponse.DailyNutrition::getCarbohydrate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(daysWithData), 2, RoundingMode.HALF_UP);

        BigDecimal avgFat = dailyData.stream()
                .map(NutritionAnalysisResponse.DailyNutrition::getFat)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(daysWithData), 2, RoundingMode.HALF_UP);

        // 按三大营养素热量贡献计算比例（蛋白质4kcal/g, 碳水4kcal/g, 脂肪9kcal/g）
        BigDecimal totalCalFromMacros = avgProtein.multiply(new BigDecimal("4"))
                .add(avgCarbohydrate.multiply(new BigDecimal("4")))
                .add(avgFat.multiply(new BigDecimal("9")));

        BigDecimal proteinRatio = BigDecimal.ZERO;
        BigDecimal carbRatio = BigDecimal.ZERO;
        BigDecimal fatRatio = BigDecimal.ZERO;

        if (totalCalFromMacros.compareTo(BigDecimal.ZERO) > 0) {
            proteinRatio = avgProtein.multiply(new BigDecimal("4")).divide(totalCalFromMacros, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            carbRatio = avgCarbohydrate.multiply(new BigDecimal("4")).divide(totalCalFromMacros, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            fatRatio = avgFat.multiply(new BigDecimal("9")).divide(totalCalFromMacros, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }

        User user = userService.getUserById(userId);
        BigDecimal targetCalories = userService.calculateTargetCalories(user);

        String aiInterpretation = generateInterpretation(avgCalories, targetCalories, proteinRatio, carbRatio, fatRatio);

        return NutritionAnalysisResponse.builder()
                .dailyData(dailyData)
                .avgCalories(avgCalories)
                .avgProtein(avgProtein)
                .avgCarbohydrate(avgCarbohydrate)
                .avgFat(avgFat)
                .proteinRatio(proteinRatio.setScale(1, RoundingMode.HALF_UP))
                .carbRatio(carbRatio.setScale(1, RoundingMode.HALF_UP))
                .fatRatio(fatRatio.setScale(1, RoundingMode.HALF_UP))
                .aiInterpretation(aiInterpretation)
                .build();
    }

    /** 根据营养数据生成AI解读建议 */
    private String generateInterpretation(BigDecimal avgCalories, BigDecimal targetCalories,
                                           BigDecimal proteinRatio, BigDecimal carbRatio, BigDecimal fatRatio) {
        StringBuilder sb = new StringBuilder();

        if (targetCalories.compareTo(BigDecimal.ZERO) == 0) {
            sb.append("请先完善您的个人健康信息（身高、体重、年龄等），以便获得个性化营养建议。");
            return sb.toString();
        }

        BigDecimal diff = avgCalories.subtract(targetCalories);
        if (diff.abs().compareTo(targetCalories.multiply(new BigDecimal("0.1"))) <= 0) {
            sb.append("您近期的平均热量摄入基本达标，");
        } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("您近期的平均热量摄入偏高，超出目标约").append(diff.setScale(0, RoundingMode.HALF_UP)).append("千卡，");
        } else {
            sb.append("您近期的平均热量摄入偏低，低于目标约").append(diff.abs().setScale(0, RoundingMode.HALF_UP)).append("千卡，");
        }

        if (proteinRatio.compareTo(new BigDecimal("15")) < 0) {
            sb.append("蛋白质摄入偏低，建议增加鸡胸肉、鱼类、豆制品等高蛋白食物；");
        } else if (proteinRatio.compareTo(new BigDecimal("25")) > 0) {
            sb.append("蛋白质摄入偏高，可适当减少肉类摄入；");
        } else {
            sb.append("蛋白质摄入合理；");
        }

        if (carbRatio.compareTo(new BigDecimal("45")) < 0) {
            sb.append("碳水化合物摄入偏低，建议适当增加全谷物主食；");
        } else if (carbRatio.compareTo(new BigDecimal("60")) > 0) {
            sb.append("碳水化合物摄入偏高，建议减少精制主食，增加蔬菜比例；");
        }

        if (fatRatio.compareTo(new BigDecimal("25")) < 0) {
            sb.append("脂肪摄入偏低，可适当增加坚果、牛油果等健康脂肪来源。");
        } else if (fatRatio.compareTo(new BigDecimal("35")) > 0) {
            sb.append("脂肪摄入偏高，建议减少油炸食品和高脂零食。");
        } else {
            sb.append("脂肪摄入在合理范围内。");
        }

        return sb.toString();
    }

    /** 营养素详细分析：各营养素每日趋势+比例+建议 */
    public NutrientDetailResponse analyzeNutrients(Long userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<DietRecord> records = dietRecordService.getRecordsByDateRange(userId, startDate, endDate);
        User user = userService.getUserById(userId);

        java.util.Map<LocalDate, NutrientDetailResponse.DailyNutrient> dailyMap = new java.util.LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyMap.put(date, NutrientDetailResponse.DailyNutrient.builder()
                    .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .protein(BigDecimal.ZERO).carbohydrate(BigDecimal.ZERO).fat(BigDecimal.ZERO)
                    .fiber(BigDecimal.ZERO).water(BigDecimal.ZERO).build());
        }

        for (DietRecord record : records) {
            NutrientDetailResponse.DailyNutrient daily = dailyMap.get(record.getRecordDate());
            if (daily != null) {
                daily.setProtein(daily.getProtein().add(record.getProtein() != null ? record.getProtein() : BigDecimal.ZERO));
                daily.setCarbohydrate(daily.getCarbohydrate().add(record.getCarbohydrate() != null ? record.getCarbohydrate() : BigDecimal.ZERO));
                daily.setFat(daily.getFat().add(record.getFat() != null ? record.getFat() : BigDecimal.ZERO));
            }
        }

        long daysWithData = dailyMap.values().stream().filter(d -> d.getProtein().add(d.getCarbohydrate()).add(d.getFat()).compareTo(BigDecimal.ZERO) > 0).count();
        if (daysWithData == 0) daysWithData = 1;

        BigDecimal avgProtein = dailyMap.values().stream().map(NutrientDetailResponse.DailyNutrient::getProtein).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(daysWithData), 2, RoundingMode.HALF_UP);
        BigDecimal avgCarb = dailyMap.values().stream().map(NutrientDetailResponse.DailyNutrient::getCarbohydrate).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(daysWithData), 2, RoundingMode.HALF_UP);
        BigDecimal avgFat = dailyMap.values().stream().map(NutrientDetailResponse.DailyNutrient::getFat).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(daysWithData), 2, RoundingMode.HALF_UP);

        BigDecimal totalCalFromMacros = avgProtein.multiply(new BigDecimal("4")).add(avgCarb.multiply(new BigDecimal("4"))).add(avgFat.multiply(new BigDecimal("9")));
        BigDecimal proteinRatio = totalCalFromMacros.compareTo(BigDecimal.ZERO) > 0 ? avgProtein.multiply(new BigDecimal("400")).divide(totalCalFromMacros, 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal carbRatio = totalCalFromMacros.compareTo(BigDecimal.ZERO) > 0 ? avgCarb.multiply(new BigDecimal("400")).divide(totalCalFromMacros, 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal fatRatio = totalCalFromMacros.compareTo(BigDecimal.ZERO) > 0 ? avgFat.multiply(new BigDecimal("900")).divide(totalCalFromMacros, 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal targetProtein = user.getTargetProtein() != null ? user.getTargetProtein() : new BigDecimal("65");
        BigDecimal targetCarb = user.getTargetCarbohydrate() != null ? user.getTargetCarbohydrate() : new BigDecimal("250");
        BigDecimal targetFat = user.getTargetFat() != null ? user.getTargetFat() : new BigDecimal("55");

        java.util.List<NutrientDetailResponse.NutrientAdvice> advices = new java.util.ArrayList<>();
        if (avgProtein.compareTo(targetProtein) < 0) {
            advices.add(NutrientDetailResponse.NutrientAdvice.builder().nutrient("蛋白质").status("不足").suggestion("建议增加鸡胸肉、鱼类、豆制品等高蛋白食物").build());
        } else if (avgProtein.compareTo(targetProtein.multiply(new BigDecimal("1.2"))) > 0) {
            advices.add(NutrientDetailResponse.NutrientAdvice.builder().nutrient("蛋白质").status("偏高").suggestion("蛋白质摄入偏高，可适当减少肉类").build());
        } else {
            advices.add(NutrientDetailResponse.NutrientAdvice.builder().nutrient("蛋白质").status("正常").suggestion("蛋白质摄入合理，继续保持").build());
        }
        if (avgCarb.compareTo(targetCarb) < 0) {
            advices.add(NutrientDetailResponse.NutrientAdvice.builder().nutrient("碳水化合物").status("不足").suggestion("建议适当增加全谷物主食").build());
        } else if (avgCarb.compareTo(targetCarb.multiply(new BigDecimal("1.2"))) > 0) {
            advices.add(NutrientDetailResponse.NutrientAdvice.builder().nutrient("碳水化合物").status("偏高").suggestion("碳水偏高，建议减少精制主食").build());
        } else {
            advices.add(NutrientDetailResponse.NutrientAdvice.builder().nutrient("碳水化合物").status("正常").suggestion("碳水摄入合理").build());
        }
        if (avgFat.compareTo(targetFat) < 0) {
            advices.add(NutrientDetailResponse.NutrientAdvice.builder().nutrient("脂肪").status("不足").suggestion("可适当增加坚果、牛油果等健康脂肪").build());
        } else if (avgFat.compareTo(targetFat.multiply(new BigDecimal("1.2"))) > 0) {
            advices.add(NutrientDetailResponse.NutrientAdvice.builder().nutrient("脂肪").status("偏高").suggestion("建议减少油炸食品和高脂零食").build());
        } else {
            advices.add(NutrientDetailResponse.NutrientAdvice.builder().nutrient("脂肪").status("正常").suggestion("脂肪摄入在合理范围内").build());
        }

        return NutrientDetailResponse.builder()
                .dailyNutrients(new java.util.ArrayList<>(dailyMap.values()))
                .summary(NutrientDetailResponse.NutrientSummary.builder()
                        .avgProtein(avgProtein).avgCarbohydrate(avgCarb).avgFat(avgFat)
                        .proteinRatio(proteinRatio).carbRatio(carbRatio).fatRatio(fatRatio)
                        .targetProtein(targetProtein).targetCarb(targetCarb).targetFat(targetFat).build())
                .advices(advices).build();
    }

    /** 热量详细分析：每日热量+时段分布+达标率 */
    public CalorieDetailResponse analyzeCalories(Long userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<DietRecord> records = dietRecordService.getRecordsByDateRange(userId, startDate, endDate);
        User user = userService.getUserById(userId);
        BigDecimal targetCal = userService.calculateTargetCalories(user);

        java.util.Map<LocalDate, BigDecimal> dailyCalMap = new java.util.LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyCalMap.put(date, BigDecimal.ZERO);
        }
        for (DietRecord record : records) {
            dailyCalMap.merge(record.getRecordDate(), record.getCalories(), BigDecimal::add);
        }

        java.util.List<CalorieDetailResponse.DailyCalorie> dailyCalories = new java.util.ArrayList<>();
        int hitDays = 0;
        BigDecimal totalDeviation = BigDecimal.ZERO;
        for (java.util.Map.Entry<LocalDate, BigDecimal> entry : dailyCalMap.entrySet()) {
            BigDecimal deviation = entry.getValue().subtract(targetCal);
            dailyCalories.add(CalorieDetailResponse.DailyCalorie.builder()
                    .date(entry.getKey().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .calories(entry.getValue()).targetCalories(targetCal)
                    .deviation(deviation).build());
            if (deviation.abs().compareTo(targetCal.multiply(new BigDecimal("0.1"))) <= 0) hitDays++;
            totalDeviation = totalDeviation.add(deviation);
        }

        java.util.Map<String, BigDecimal> mealCalMap = new java.util.LinkedHashMap<>();
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
        java.util.List<CalorieDetailResponse.MealCalorie> mealDistribution = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, BigDecimal> entry : mealCalMap.entrySet()) {
            BigDecimal ratio = totalMealCal.compareTo(BigDecimal.ZERO) > 0 ? entry.getValue().multiply(new BigDecimal("100")).divide(totalMealCal, 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
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

    /** 目标完成度分析：各营养素完成百分比+建议 */
    public GoalCompletionResponse analyzeGoalCompletion(Long userId) {
        User user = userService.getUserById(userId);
        java.util.List<DietRecord> todayRecords = dietRecordService.getTodayRecords(userId);

        BigDecimal actualCal = todayRecords.stream().map(DietRecord::getCalories).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualProtein = todayRecords.stream().map(r -> r.getProtein() != null ? r.getProtein() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualCarb = todayRecords.stream().map(r -> r.getCarbohydrate() != null ? r.getCarbohydrate() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualFat = todayRecords.stream().map(r -> r.getFat() != null ? r.getFat() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal targetCal = userService.calculateTargetCalories(user);
        BigDecimal targetProtein = user.getTargetProtein() != null ? user.getTargetProtein() : new BigDecimal("65");
        BigDecimal targetCarb = user.getTargetCarbohydrate() != null ? user.getTargetCarbohydrate() : new BigDecimal("250");
        BigDecimal targetFat = user.getTargetFat() != null ? user.getTargetFat() : new BigDecimal("55");

        java.util.List<GoalCompletionResponse.GoalItem> goals = new java.util.ArrayList<>();
        goals.add(buildGoalItem("热量", targetCal, actualCal));
        goals.add(buildGoalItem("蛋白质", targetProtein, actualProtein));
        goals.add(buildGoalItem("碳水化合物", targetCarb, actualCarb));
        goals.add(buildGoalItem("脂肪", targetFat, actualFat));

        BigDecimal overallCompletion = goals.stream().map(GoalCompletionResponse.GoalItem::getCompletion).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(goals.size()), 0, RoundingMode.HALF_UP);

        String overallStatus;
        if (overallCompletion.compareTo(new BigDecimal("90")) >= 0) overallStatus = "优秀";
        else if (overallCompletion.compareTo(new BigDecimal("70")) >= 0) overallStatus = "良好";
        else if (overallCompletion.compareTo(new BigDecimal("50")) >= 0) overallStatus = "一般";
        else overallStatus = "不足";

        java.util.List<String> suggestions = new java.util.ArrayList<>();
        for (GoalCompletionResponse.GoalItem goal : goals) {
            if ("不足".equals(goal.getStatus())) {
                suggestions.add(goal.getName() + "还差" + goal.getGap().setScale(0, RoundingMode.HALF_UP) + "，建议增加摄入");
            } else if ("超标".equals(goal.getStatus())) {
                suggestions.add(goal.getName() + "超出" + goal.getGap().abs().setScale(0, RoundingMode.HALF_UP) + "，建议控制摄入");
            }
        }
        if (suggestions.isEmpty()) {
            suggestions.add("各营养素摄入均衡，继续保持！");
        }

        return GoalCompletionResponse.builder()
                .goals(goals).overallCompletion(overallCompletion)
                .overallStatus(overallStatus).suggestions(suggestions).build();
    }

    private GoalCompletionResponse.GoalItem buildGoalItem(String name, BigDecimal target, BigDecimal actual) {
        BigDecimal completion = target.compareTo(BigDecimal.ZERO) > 0 ? actual.multiply(new BigDecimal("100")).divide(target, 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        if (completion.compareTo(new BigDecimal("100")) > 0) completion = new BigDecimal("100");
        BigDecimal gap = target.subtract(actual);
        String status;
        if (completion.compareTo(new BigDecimal("90")) >= 0) status = "达标";
        else if (completion.compareTo(new BigDecimal("70")) >= 0) status = "接近";
        else if (completion.compareTo(new BigDecimal("30")) >= 0) status = "不足";
        else status = "严重不足";
        if (gap.compareTo(BigDecimal.ZERO) < 0) status = "超标";
        return GoalCompletionResponse.GoalItem.builder().name(name).target(target).actual(actual).completion(completion).status(status).gap(gap).build();
    }

    /** 营养报告：综合分析+评分+关键发现+建议 */
    public NutritionReportResponse generateReport(Long userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<DietRecord> records = dietRecordService.getRecordsByDateRange(userId, startDate, endDate);
        User user = userService.getUserById(userId);

        long daysWithData = records.stream().map(DietRecord::getRecordDate).distinct().count();
        if (daysWithData == 0) daysWithData = 1;

        BigDecimal totalCal = records.stream().map(DietRecord::getCalories).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProtein = records.stream().map(r -> r.getProtein() != null ? r.getProtein() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCarb = records.stream().map(r -> r.getCarbohydrate() != null ? r.getCarbohydrate() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFat = records.stream().map(r -> r.getFat() != null ? r.getFat() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgCalories = totalCal.divide(BigDecimal.valueOf(daysWithData), 0, RoundingMode.HALF_UP);
        BigDecimal avgProtein = totalProtein.divide(BigDecimal.valueOf(daysWithData), 1, RoundingMode.HALF_UP);
        BigDecimal avgCarb = totalCarb.divide(BigDecimal.valueOf(daysWithData), 1, RoundingMode.HALF_UP);
        BigDecimal avgFat = totalFat.divide(BigDecimal.valueOf(daysWithData), 1, RoundingMode.HALF_UP);

        BigDecimal totalCalFromMacros = avgProtein.multiply(new BigDecimal("4")).add(avgCarb.multiply(new BigDecimal("4"))).add(avgFat.multiply(new BigDecimal("9")));
        BigDecimal proteinRatio = totalCalFromMacros.compareTo(BigDecimal.ZERO) > 0 ? avgProtein.multiply(new BigDecimal("400")).divide(totalCalFromMacros, 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal carbRatio = totalCalFromMacros.compareTo(BigDecimal.ZERO) > 0 ? avgCarb.multiply(new BigDecimal("400")).divide(totalCalFromMacros, 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal fatRatio = totalCalFromMacros.compareTo(BigDecimal.ZERO) > 0 ? avgFat.multiply(new BigDecimal("900")).divide(totalCalFromMacros, 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal targetCal = userService.calculateTargetCalories(user);
        int calorieScore = calculateItemScore(avgCalories, targetCal);
        BigDecimal targetProtein = user.getTargetProtein() != null ? user.getTargetProtein() : new BigDecimal("65");
        int proteinScore = calculateItemScore(avgProtein, targetProtein);
        BigDecimal targetCarb = user.getTargetCarbohydrate() != null ? user.getTargetCarbohydrate() : new BigDecimal("250");
        int carbScore = calculateItemScore(avgCarb, targetCarb);
        BigDecimal targetFat = user.getTargetFat() != null ? user.getTargetFat() : new BigDecimal("55");
        int fatScore = calculateItemScore(avgFat, targetFat);

        int nutritionScore = (calorieScore * 30 + proteinScore * 30 + carbScore * 20 + fatScore * 20) / 100;

        String calorieEval = evaluateLevel(calorieScore);
        String proteinEval = evaluateLevel(proteinScore);
        String carbEval = evaluateLevel(carbScore);
        String fatEval = evaluateLevel(fatScore);

        java.util.List<String> keyFindings = new java.util.ArrayList<>();
        java.util.List<String> recommendations = new java.util.ArrayList<>();

        if (calorieScore >= 80) keyFindings.add("热量摄入基本达标");
        else if (calorieScore >= 50) keyFindings.add("热量摄入略有偏差");
        else keyFindings.add("热量摄入严重偏离目标");

        if (proteinScore < 70) { keyFindings.add("蛋白质摄入不足"); recommendations.add("增加优质蛋白：鸡胸肉、鱼类、鸡蛋、豆制品"); }
        if (carbScore < 70) { keyFindings.add("碳水化合物摄入不足"); recommendations.add("适当增加全谷物、燕麦、红薯等主食"); }
        if (fatScore < 70) { keyFindings.add("脂肪摄入不足"); recommendations.add("适量补充坚果、牛油果等健康脂肪"); }
        if (fatRatio.compareTo(new BigDecimal("35")) > 0) { keyFindings.add("脂肪供能比例偏高"); recommendations.add("减少油炸食品，控制烹饪用油"); }
        if (proteinRatio.compareTo(new BigDecimal("15")) < 0) { keyFindings.add("蛋白质供能比例偏低"); recommendations.add("每餐保证一份优质蛋白来源"); }

        if (recommendations.isEmpty()) recommendations.add("当前饮食结构合理，继续保持均衡饮食");

        return NutritionReportResponse.builder()
                .period(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + " ~ " + endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .days(days).avgCalories(avgCalories).avgProtein(avgProtein).avgCarbohydrate(avgCarb).avgFat(avgFat)
                .proteinRatio(proteinRatio).carbRatio(carbRatio).fatRatio(fatRatio)
                .nutritionScore(BigDecimal.valueOf(nutritionScore))
                .calorieEvaluation(calorieEval).proteinEvaluation(proteinEval).carbEvaluation(carbEval).fatEvaluation(fatEval)
                .keyFindings(keyFindings).recommendations(recommendations).build();
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
}