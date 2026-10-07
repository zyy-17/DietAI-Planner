package com.zyyqq.controller.admin;

import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端数据看板接口。
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminService adminService;

    /** 核心指标概览：用户数、食物数、饮食记录数、AI调用数等 */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getOverview() {
        return ApiResponse.success(adminService.getOverview());
    }

    /** 近 N 天饮食记录趋势 */
    @GetMapping("/trend")
    public ApiResponse<List<Map<String, Object>>> getTrend(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(adminService.getDietRecordTrend(days));
    }
}
