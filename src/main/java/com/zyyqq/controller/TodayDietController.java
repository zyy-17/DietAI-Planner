package com.zyyqq.controller;

import com.zyyqq.dto.request.AddDietRecordRequest;
import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.dto.response.TodayDietOverviewResponse;
import com.zyyqq.entity.DietRecord;
import com.zyyqq.entity.User;
import com.zyyqq.service.DietRecordService;
import com.zyyqq.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diet")
@RequiredArgsConstructor
public class TodayDietController {

    private final DietRecordService dietRecordService;
    private final UserService userService;

    @GetMapping("/today")
    public ApiResponse<TodayDietOverviewResponse> getTodayOverview(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(dietRecordService.getTodayOverview(userId));
    }

    @GetMapping("/today/records")
    public ApiResponse<List<DietRecord>> getTodayRecords(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(dietRecordService.getTodayRecords(userId));
    }

    @PostMapping("/today/add")
    public ApiResponse<DietRecord> addDietRecord(Authentication authentication,
                                                 @RequestBody AddDietRecordRequest request) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(dietRecordService.addDietRecord(userId, request));
    }

    @PostMapping("/today/add-batch")
    public ApiResponse<List<DietRecord>> addDietRecordBatch(Authentication authentication,
                                                            @RequestBody List<AddDietRecordRequest> requests) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(dietRecordService.addDietRecordBatch(userId, requests));
    }

    /** 更新用户的每日营养目标（热量/蛋白质/碳水/脂肪） */
    @PutMapping("/today/target")
    public ApiResponse<Void> updateTodayTarget(Authentication authentication,
                                               @RequestBody Map<String, BigDecimal> targetMap) {
        Long userId = getUserId(authentication);
        userService.updateTarget(userId, targetMap);
        return ApiResponse.success("目标已更新", null);
    }

    /** 删除指定饮食记录 */
    @DeleteMapping("/record/{id}")
    public ApiResponse<Void> deleteDietRecord(Authentication authentication,
                                              @PathVariable Long id) {
        Long userId = getUserId(authentication);
        dietRecordService.deleteDietRecord(id, userId);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/records")
    public ApiResponse<List<DietRecord>> getRecordsByDate(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = getUserId(authentication);
        if (date != null) {
            return ApiResponse.success(dietRecordService.getRecordsByDate(userId, date));
        }
        if (startDate != null && endDate != null) {
            return ApiResponse.success(dietRecordService.getRecordsByDateRange(userId, startDate, endDate));
        }
        return ApiResponse.success(dietRecordService.getAllRecords(userId));
    }

    @GetMapping("/records/week")
    public ApiResponse<List<DietRecord>> getWeekRecords(Authentication authentication) {
        Long userId = getUserId(authentication);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        return ApiResponse.success(dietRecordService.getRecordsByDateRange(userId, start, end));
    }

    @GetMapping("/records/month")
    public ApiResponse<List<DietRecord>> getMonthRecords(Authentication authentication) {
        Long userId = getUserId(authentication);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(29);
        return ApiResponse.success(dietRecordService.getRecordsByDateRange(userId, start, end));
    }

    @GetMapping("/stats")
    public ApiResponse<java.util.Map<String, Object>> getStats(
            Authentication authentication,
            @RequestParam(defaultValue = "week") String period) {
        Long userId = getUserId(authentication);
        User user = userService.getUserById(userId);
        BigDecimal targetCal = userService.calculateTargetCalories(user);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate;
        switch (period) {
            case "month": startDate = endDate.withDayOfMonth(1); break;
            case "year": startDate = endDate.withDayOfYear(1); break;
            default: startDate = endDate.minusDays(6); break;
        }

        List<DietRecord> records = dietRecordService.getRecordsByDateRange(userId, startDate, endDate);

        java.util.Map<LocalDate, BigDecimal> dailyCalMap = new java.util.LinkedHashMap<>();
        java.util.Map<LocalDate, BigDecimal> dailyProteinMap = new java.util.LinkedHashMap<>();
        java.util.Map<LocalDate, BigDecimal> dailyCarbMap = new java.util.LinkedHashMap<>();
        java.util.Map<LocalDate, BigDecimal> dailyFatMap = new java.util.LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyCalMap.put(date, BigDecimal.ZERO);
            dailyProteinMap.put(date, BigDecimal.ZERO);
            dailyCarbMap.put(date, BigDecimal.ZERO);
            dailyFatMap.put(date, BigDecimal.ZERO);
        }
        for (DietRecord r : records) {
            dailyCalMap.merge(r.getRecordDate(), r.getCalories(), BigDecimal::add);
            dailyProteinMap.merge(r.getRecordDate(), r.getProtein() != null ? r.getProtein() : BigDecimal.ZERO, BigDecimal::add);
            dailyCarbMap.merge(r.getRecordDate(), r.getCarbohydrate() != null ? r.getCarbohydrate() : BigDecimal.ZERO, BigDecimal::add);
            dailyFatMap.merge(r.getRecordDate(), r.getFat() != null ? r.getFat() : BigDecimal.ZERO, BigDecimal::add);
        }

        java.util.List<java.util.Map<String, Object>> dailyRecords = new java.util.ArrayList<>();
        int onTargetDays = 0;
        for (java.util.Map.Entry<LocalDate, BigDecimal> entry : dailyCalMap.entrySet()) {
            java.util.Map<String, Object> day = new java.util.LinkedHashMap<>();
            day.put("date", entry.getKey().toString());
            day.put("totalCalories", entry.getValue());
            dailyRecords.add(day);
            if (targetCal.compareTo(BigDecimal.ZERO) > 0 && entry.getValue().compareTo(BigDecimal.ZERO) > 0
                    && entry.getValue().subtract(targetCal).abs().compareTo(targetCal.multiply(new BigDecimal("0.1"))) <= 0) {
                onTargetDays++;
            }
        }

        long daysWithData = dailyCalMap.values().stream().filter(c -> c.compareTo(BigDecimal.ZERO) > 0).count();
        if (daysWithData == 0) daysWithData = 1;

        BigDecimal totalCal = dailyCalMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProtein = dailyProteinMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCarb = dailyCarbMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFat = dailyFatMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalDays", daysWithData);
        stats.put("avgCalories", totalCal.divide(BigDecimal.valueOf(daysWithData), 0, java.math.RoundingMode.HALF_UP));
        stats.put("avgProtein", totalProtein.divide(BigDecimal.valueOf(daysWithData), 1, java.math.RoundingMode.HALF_UP));
        stats.put("avgCarbohydrate", totalCarb.divide(BigDecimal.valueOf(daysWithData), 1, java.math.RoundingMode.HALF_UP));
        stats.put("avgFat", totalFat.divide(BigDecimal.valueOf(daysWithData), 1, java.math.RoundingMode.HALF_UP));
        stats.put("onTargetDays", onTargetDays);
        stats.put("dailyRecords", dailyRecords);

        return ApiResponse.success(stats);
    }

    /** 从认证信息中提取用户ID */
    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}