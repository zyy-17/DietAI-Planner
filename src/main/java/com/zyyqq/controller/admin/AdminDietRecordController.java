package com.zyyqq.controller.admin;

import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.entity.DietRecord;
import com.zyyqq.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端饮食记录管理接口（对应客户端"饮食记录 / 饮食统计"模块）。
 */
@RestController
@RequestMapping("/api/admin/diet-records")
@RequiredArgsConstructor
public class AdminDietRecordController {

    private final AdminService adminService;

    /** 分页查询全部饮食记录，可按用户ID、日期筛选 */
    @GetMapping
    public ApiResponse<Page<DietRecord>> getDietRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.getDietRecords(userId, date, page, size));
    }

    /** 删除任意一条饮食记录 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDietRecord(@PathVariable Long id) {
        adminService.deleteDietRecord(id);
        return ApiResponse.success("删除成功", null);
    }
}
