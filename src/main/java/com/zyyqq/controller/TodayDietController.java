package com.zyyqq.controller;

import com.zyyqq.dto.request.AddDietRecordRequest;
import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.dto.response.TodayDietOverviewResponse;
import com.zyyqq.entity.DietRecord;
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
        List<DietRecord> results = new java.util.ArrayList<>();
        for (AddDietRecordRequest request : requests) {
            results.add(dietRecordService.addDietRecord(userId, request));
        }
        return ApiResponse.success(results);
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

    @GetMapping("/stats")
    public ApiResponse<java.util.Map<String, Object>> getStats(Authentication authentication) {
        Long userId = getUserId(authentication);
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalDays", dietRecordService.getRecordDayCount(userId));
        stats.put("totalCalories", dietRecordService.getTotalCalories(userId));
        return ApiResponse.success(stats);
    }

    /** 从认证信息中提取用户ID */
    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}