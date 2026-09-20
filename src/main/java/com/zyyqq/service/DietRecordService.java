package com.zyyqq.service;

import com.zyyqq.dto.request.AddDietRecordRequest;
import com.zyyqq.dto.response.TodayDietOverviewResponse;
import com.zyyqq.entity.DietRecord;
import com.zyyqq.entity.Food;
import com.zyyqq.entity.User;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.DietRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DietRecordService {

    private final DietRecordRepository dietRecordRepository;
    private final FoodService foodService;
    private final UserService userService;

    @Transactional
    public DietRecord addDietRecord(Long userId, AddDietRecordRequest request) {
        Food food = foodService.getFoodById(request.getFoodId());

        BigDecimal ratio = request.getAmount().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        BigDecimal calories = food.getCalories().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal protein = food.getProtein() != null ? food.getProtein().multiply(ratio).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal carbohydrate = food.getCarbohydrate() != null ? food.getCarbohydrate().multiply(ratio).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal fat = food.getFat() != null ? food.getFat().multiply(ratio).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        DietRecord record = DietRecord.builder()
                .userId(userId)
                .foodId(request.getFoodId())
                .mealType(request.getMealType())
                .amount(request.getAmount())
                .calories(calories)
                .protein(protein)
                .carbohydrate(carbohydrate)
                .fat(fat)
                .recordDate(LocalDate.now())
                .build();

        record = dietRecordRepository.save(record);
        record.setFoodName(food.getName());
        return record;
    }

    @Transactional
    public List<DietRecord> addDietRecordBatch(Long userId, List<AddDietRecordRequest> requests) {
        List<Long> foodIds = requests.stream().map(AddDietRecordRequest::getFoodId).distinct().toList();
        Map<Long, Food> foodMap = new java.util.HashMap<>();
        for (Long foodId : foodIds) {
            foodMap.put(foodId, foodService.getFoodById(foodId));
        }

        List<DietRecord> results = new java.util.ArrayList<>();
        for (AddDietRecordRequest request : requests) {
            Food food = foodMap.get(request.getFoodId());
            if (food == null) continue;

            BigDecimal ratio = request.getAmount().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal calories = food.getCalories().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal protein = food.getProtein() != null ? food.getProtein().multiply(ratio).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal carbohydrate = food.getCarbohydrate() != null ? food.getCarbohydrate().multiply(ratio).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal fat = food.getFat() != null ? food.getFat().multiply(ratio).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            DietRecord record = DietRecord.builder()
                    .userId(userId)
                    .foodId(request.getFoodId())
                    .mealType(request.getMealType())
                    .amount(request.getAmount())
                    .calories(calories)
                    .protein(protein)
                    .carbohydrate(carbohydrate)
                    .fat(fat)
                    .recordDate(LocalDate.now())
                    .build();
            record = dietRecordRepository.save(record);
            record.setFoodName(food.getName());
            results.add(record);
        }
        log.info("批量添加饮食记录: userId={}, count={}", userId, results.size());
        return results;
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

    private void enrichFoodNames(List<DietRecord> records) {
        if (records.isEmpty()) return;
        List<Long> foodIds = records.stream().map(DietRecord::getFoodId).distinct().toList();
        Map<Long, String> foodNameMap = foodService.getFoodNamesByIds(foodIds);
        for (DietRecord record : records) {
            record.setFoodName(foodNameMap.getOrDefault(record.getFoodId(), "未知食物"));
        }
    }
}