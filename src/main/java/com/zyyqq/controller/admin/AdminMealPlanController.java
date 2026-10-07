package com.zyyqq.controller.admin;

import com.zyyqq.dto.request.MealPlanRejectRequest;
import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.dto.response.MealPlanDetailVO;
import com.zyyqq.dto.response.MealPlanVO;
import com.zyyqq.service.MealPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 食谱广场的审核接口（仅管理员，走 {@code /api/admin/**} 的 ROLE_ADMIN 约束）。
 */
@RestController
@RequestMapping("/api/admin/meal-plans")
@RequiredArgsConstructor
public class AdminMealPlanController {

    private final MealPlanService mealPlanService;

    /** 按发布状态分页；默认看待审核的 */
    @GetMapping
    public ApiResponse<Page<MealPlanVO>> list(@RequestParam(required = false) String status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(mealPlanService.adminPage(status, page, size));
    }

    /** 待审核数量（后台首页/角标用） */
    @GetMapping("/pending-count")
    public ApiResponse<Long> pendingCount() {
        return ApiResponse.success(mealPlanService.countPending());
    }

    /** 查看某份食谱的完整内容（含逐天明细，待审核的也能看） */
    @GetMapping("/{id}")
    public ApiResponse<MealPlanDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(mealPlanService.adminDetail(id));
    }

    /** 审核通过 → 上架广场 */
    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id) {
        mealPlanService.approve(id);
        return ApiResponse.success("已通过", null);
    }

    /** 驳回并附原因 */
    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestBody(required = false) MealPlanRejectRequest request) {
        mealPlanService.reject(id, request == null ? null : request.getReason());
        return ApiResponse.success("已驳回", null);
    }

    /** 管理员下架已上架的食谱 */
    @PostMapping("/{id}/takedown")
    public ApiResponse<Void> takeDown(@PathVariable Long id) {
        mealPlanService.adminTakeDown(id);
        return ApiResponse.success("已下架", null);
    }
}