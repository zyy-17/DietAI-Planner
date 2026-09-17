package com.zyyqq.controller;

import com.zyyqq.dto.request.AddDietRecordRequest;
import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.dto.response.TodayDietOverviewResponse;
import com.zyyqq.entity.DietRecord;
import com.zyyqq.service.DietRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/diet")
@RequiredArgsConstructor
public class TodayDietController {

    private final DietRecordService dietRecordService;

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

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}