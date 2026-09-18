package com.zyyqq.service;

import com.zyyqq.dto.request.AddDietRecordRequest;
import com.zyyqq.dto.response.TodayDietOverviewResponse;
import com.zyyqq.entity.DietRecord;
import com.zyyqq.entity.Food;
import com.zyyqq.entity.User;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.DietRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DietRecordService {

    private final DietRecordRepository dietRecordRepository;
    private final FoodService foodService;
    private final UserService userService;

    /** 添加饮食记录，根据食物营养和食用量计算实际摄入 */
    @Transactional
    public DietRecord addDietRecord(Long userId, AddDietRecordRequest request) {
        Food food = foodService.getFoodById(request.getFoodId());

        // 按每100g为基准计算实际食用比例
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

    /** 获取用户今日饮食记录 */
    public List<DietRecord> getTodayRecords(Long userId) {
        List<DietRecord> records = dietRecordRepository.findByUserIdAndRecordDateOrderByCreatedAtDesc(userId, LocalDate.now());
        enrichFoodNames(records);
        return records;
    }

    /** 获取用户指定日期的饮食记录 */
    public List<DietRecord> getRecordsByDate(Long userId, LocalDate date) {
        List<DietRecord> records = dietRecordRepository.findByUserIdAndRecordDateOrderByCreatedAtDesc(userId, date);
        enrichFoodNames(records);
        return records;
    }

    /** 获取用户日期范围内的饮食记录 */
    public List<DietRecord> getRecordsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        List<DietRecord> records = dietRecordRepository.findByUserIdAndRecordDateBetweenOrderByRecordDateDescCreatedAtDesc(userId, startDate, endDate);
        enrichFoodNames(records);
        return records;
    }

    /** 获取用户所有饮食记录 */
    public List<DietRecord> getAllRecords(Long userId) {
        List<DietRecord> records = dietRecordRepository.findAllByUserIdOrderByDateDesc(userId);
        enrichFoodNames(records);
        return records;
    }

    /** 获取今日饮食概览，包含总摄入和目标对比 */
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

        // 若用户未设置目标，则按推荐公式计算
        BigDecimal targetCalories = user.getTargetCalories() != null ? user.getTargetCalories() : userService.calculateTargetCalories(user);
        BigDecimal remainingCalories = targetCalories.subtract(totalCalories);

        // 若未设置各营养素目标，按热量比例推算：蛋白质20%、碳水50%、脂肪30%
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

    /** 删除指定饮食记录 */
    @Transactional
    public void deleteDietRecord(Long id, Long userId) {
        dietRecordRepository.deleteByIdAndUserId(id, userId);
    }

    /** 统计用户记录天数 */
    public long getRecordDayCount(Long userId) {
        return dietRecordRepository.countDistinctRecordDatesByUserId(userId);
    }

    /** 统计用户总摄入热量 */
    public Double getTotalCalories(Long userId) {
        return dietRecordRepository.sumCaloriesByUserId(userId);
    }

    /** 为记录列表填充食物名称（DietRecord不关联Food实体，需手动补充） */
    private void enrichFoodNames(List<DietRecord> records) {
        for (DietRecord record : records) {
            try {
                Food food = foodService.getFoodById(record.getFoodId());
                record.setFoodName(food.getName());
            } catch (Exception e) {
                record.setFoodName("未知食物");
            }
        }
    }
}