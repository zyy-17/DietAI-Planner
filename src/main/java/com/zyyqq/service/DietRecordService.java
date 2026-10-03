package com.zyyqq.service;

import com.zyyqq.dto.request.AddCustomDietRecordRequest;
import com.zyyqq.dto.request.AddDietRecordRequest;
import com.zyyqq.dto.response.TodayDietOverviewResponse;
import com.zyyqq.entity.DietRecord;
import com.zyyqq.entity.Food;
import com.zyyqq.entity.User;
import com.zyyqq.entity.UserCustomFood;
import com.zyyqq.repository.DietRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DietRecordService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** 食物来源：公共食物库 */
    private static final String SOURCE_SYSTEM = "system";
    /** 食物来源：用户自定义食物 */
    private static final String SOURCE_USER = "user";

    private final DietRecordRepository dietRecordRepository;
    private final FoodService foodService;
    private final UserCustomFoodService userCustomFoodService;
    private final UserService userService;

    @Transactional
    public DietRecord addDietRecord(Long userId, AddDietRecordRequest request) {
        if (isCustom(request.getFoodSource())) {
            // 自定义食物：校验归属后直接从 user_custom_food 取数
            UserCustomFood custom = userCustomFoodService.getOwned(request.getFoodId(), userId);
            return saveRecord(userId, custom.getId(), SOURCE_USER, custom.getName(),
                    custom.getCalories(), custom.getProtein(), custom.getCarbohydrate(), custom.getFat(),
                    request.getMealType(), request.getAmount());
        }
        Food food = foodService.getFoodById(request.getFoodId());
        return saveRecord(userId, food.getId(), SOURCE_SYSTEM, food.getName(),
                food.getCalories(), food.getProtein(), food.getCarbohydrate(), food.getFat(),
                request.getMealType(), request.getAmount());
    }

    @Transactional
    public List<DietRecord> addDietRecordBatch(Long userId, List<AddDietRecordRequest> requests) {
        List<DietRecord> results = new ArrayList<>();
        for (AddDietRecordRequest request : requests) {
            results.add(addDietRecord(userId, request));
        }
        log.info("批量添加饮食记录: userId={}, count={}", userId, results.size());
        return results;
    }

    /**
     * 记录饮食时添加自定义食物：写入用户的私有自定义食物表并计入当前餐次。
     * 该食物不会进入公共食物库（food 表），其他用户无法看到。
     */
    @Transactional
    public DietRecord addCustomDietRecord(Long userId, AddCustomDietRecordRequest request) {
        UserCustomFood custom = userCustomFoodService.findOrCreate(
                userId,
                request.getFoodName(),
                request.getCategoryId(),
                request.getCalories(),
                request.getProtein(),
                request.getCarbohydrate(),
                request.getFat(),
                request.getFiber());
        DietRecord record = saveRecord(userId, custom.getId(), SOURCE_USER, custom.getName(),
                custom.getCalories(), custom.getProtein(), custom.getCarbohydrate(), custom.getFat(),
                request.getMealType(), request.getAmount());
        log.info("添加自定义食物并记录饮食: userId={}, foodName={}, mealType={}",
                userId, custom.getName(), request.getMealType());
        return record;
    }

    /** 按每 100g 营养值换算实际份量并保存饮食记录 */
    private DietRecord saveRecord(Long userId, Long foodId, String foodSource, String foodName,
                                  BigDecimal caloriesPer100, BigDecimal proteinPer100,
                                  BigDecimal carbohydratePer100, BigDecimal fatPer100,
                                  String mealType, BigDecimal amount) {
        BigDecimal ratio = amount.divide(HUNDRED, 4, RoundingMode.HALF_UP);

        DietRecord record = DietRecord.builder()
                .userId(userId)
                .foodId(foodId)
                .foodSource(foodSource)
                .mealType(mealType)
                .amount(amount)
                .calories(scaleByAmount(caloriesPer100, ratio))
                .protein(scaleByAmount(proteinPer100, ratio))
                .carbohydrate(scaleByAmount(carbohydratePer100, ratio))
                .fat(scaleByAmount(fatPer100, ratio))
                .recordDate(LocalDate.now())
                .build();

        DietRecord saved = dietRecordRepository.save(record);
        saved.setFoodName(foodName);
        return saved;
    }

    private BigDecimal scaleByAmount(BigDecimal per100, BigDecimal ratio) {
        return per100 != null ? per100.multiply(ratio).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private boolean isCustom(String foodSource) {
        return SOURCE_USER.equals(foodSource);
    }

    public List<DietRecord> getTodayRecords(Long userId) {
        List<DietRecord> records = dietRecordRepository.findByUserIdAndRecordDateOrderByCreatedAtDesc(userId, LocalDate.now());
        enrichFoodNames(records);
        return records;
    }

    public List<DietRecord> getRecordsByDate(Long userId, LocalDate date) {
        List<DietRecord> records = dietRecordRepository.findByUserIdAndRecordDateOrderByCreatedAtDesc(userId, date);
        enrichFoodNames(records);
        return records;
    }

    public List<DietRecord> getRecordsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        List<DietRecord> records = dietRecordRepository.findByUserIdAndRecordDateBetweenOrderByRecordDateDescCreatedAtDesc(userId, startDate, endDate);
        enrichFoodNames(records);
        return records;
    }

    public List<DietRecord> getAllRecords(Long userId) {
        List<DietRecord> records = dietRecordRepository.findAllByUserIdOrderByDateDesc(userId);
        enrichFoodNames(records);
        return records;
    }

    public TodayDietOverviewResponse getTodayOverview(Long userId) {
        List<DietRecord> records = dietRecordRepository.findByUserIdAndRecordDateOrderByCreatedAtDesc(userId, LocalDate.now());

        BigDecimal totalCalories = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalCarbohydrate = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;

        for (DietRecord record : records) {
            totalCalories = totalCalories.add(record.getCalories());
            totalProtein = totalProtein.add(record.getProtein() != null ? record.getProtein() : BigDecimal.ZERO);
            totalCarbohydrate = totalCarbohydrate.add(record.getCarbohydrate() != null ? record.getCarbohydrate() : BigDecimal.ZERO);
            totalFat = totalFat.add(record.getFat() != null ? record.getFat() : BigDecimal.ZERO);
        }

        User user = userService.getUserById(userId);

        BigDecimal targetCalories = user.getTargetCalories() != null ? user.getTargetCalories() : userService.calculateTargetCalories(user);
        BigDecimal remainingCalories = targetCalories.subtract(totalCalories);

        BigDecimal targetProtein = user.getTargetProtein() != null ? user.getTargetProtein() : targetCalories.multiply(new BigDecimal("0.20")).divide(new BigDecimal("4"), 0, RoundingMode.HALF_UP);
        BigDecimal targetCarbohydrate = user.getTargetCarbohydrate() != null ? user.getTargetCarbohydrate() : targetCalories.multiply(new BigDecimal("0.50")).divide(new BigDecimal("4"), 0, RoundingMode.HALF_UP);
        BigDecimal targetFat = user.getTargetFat() != null ? user.getTargetFat() : targetCalories.multiply(new BigDecimal("0.30")).divide(new BigDecimal("9"), 0, RoundingMode.HALF_UP);

        return TodayDietOverviewResponse.builder()
                .totalCalories(totalCalories)
                .targetCalories(targetCalories)
                .remainingCalories(remainingCalories)
                .totalProtein(totalProtein)
                .totalCarbohydrate(totalCarbohydrate)
                .totalFat(totalFat)
                .targetProtein(targetProtein)
                .targetCarbohydrate(targetCarbohydrate)
                .targetFat(targetFat)
                .build();
    }

    @Transactional
    public void deleteDietRecord(Long id, Long userId) {
        dietRecordRepository.deleteByIdAndUserId(id, userId);
    }

    public long getRecordDayCount(Long userId) {
        return dietRecordRepository.countDistinctRecordDatesByUserId(userId);
    }

    public Double getTotalCalories(Long userId) {
        return dietRecordRepository.sumCaloriesByUserId(userId);
    }

    /**
     * 填充食物名称。
     * 系统食物与自定义食物分属两张表、ID 各自独立，必须按 food_source 分别查询。
     */
    private void enrichFoodNames(List<DietRecord> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> systemIds = new ArrayList<>();
        List<Long> customIds = new ArrayList<>();
        for (DietRecord record : records) {
            if (isCustom(record.getFoodSource())) {
                customIds.add(record.getFoodId());
            } else {
                systemIds.add(record.getFoodId());
            }
        }
        Map<Long, String> systemNames = foodService.getFoodNamesByIds(systemIds.stream().distinct().toList());
        Map<Long, String> customNames = userCustomFoodService.getNamesByIds(customIds.stream().distinct().toList());

        for (DietRecord record : records) {
            Map<Long, String> names = isCustom(record.getFoodSource()) ? customNames : systemNames;
            record.setFoodName(names.getOrDefault(record.getFoodId(), "未知食物"));
        }
    }

    @Transactional
    public List<DietRecord> addDietRecordByNames(Long userId, String mealType, List<Map<String, Object>> foodItems) {
        List<Food> allFoods = foodService.getAllApprovedFoods();
        Map<String, Food> foodNameMap = new java.util.HashMap<>();
        for (Food food : allFoods) {
            foodNameMap.put(food.getName(), food);
        }

        List<DietRecord> results = new java.util.ArrayList<>();
        for (Map<String, Object> item : foodItems) {
            String foodName = (String) item.get("foodName");
            BigDecimal amount = new BigDecimal(String.valueOf(item.getOrDefault("amount", 100)));

            Food food = foodNameMap.get(foodName);
            if (food == null) {
                for (Food f : allFoods) {
                    if (f.getName().contains(foodName) || foodName.contains(f.getName())) {
                        food = f;
                        break;
                    }
                }
            }
            if (food == null) {
                log.warn("AI建议应用: 食物[{}]未在数据库中找到，跳过", foodName);
                continue;
            }

            results.add(saveRecord(userId, food.getId(), SOURCE_SYSTEM, food.getName(),
                    food.getCalories(), food.getProtein(), food.getCarbohydrate(), food.getFat(),
                    mealType, amount));
        }
        log.info("AI建议应用饮食记录: userId={}, mealType={}, count={}", userId, mealType, results.size());
        return results;
    }
}
