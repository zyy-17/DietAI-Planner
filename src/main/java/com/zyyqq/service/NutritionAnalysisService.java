package com.zyyqq.service;

import com.zyyqq.dto.response.NutritionAnalysisResponse;
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
}