package com.zyyqq.controller.admin;

import com.zyyqq.dto.request.AddFoodRequest;
import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.entity.Food;
import com.zyyqq.service.FoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端食物库管理接口（对应客户端"食物库"模块）。
 * 仅管理系统食物（source='system'），用户自定义食物不在管理端出现。
 * 支持新增、编辑、审核（通过/驳回）、删除。
 */
@RestController
@RequestMapping("/api/admin/foods")
@RequiredArgsConstructor
public class AdminFoodController {

    private final FoodService foodService;

    /** 分页查询系统食物库，可按状态（approved/pending/rejected）与名称关键字筛选 */
    @GetMapping
    public ApiResponse<Page<Food>> getFoods(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(foodService.getAllFoodsForAdmin(status, keyword, page, size));
    }

    /** 查询单个食物详情 */
    @GetMapping("/{id}")
    public ApiResponse<Food> getFoodById(@PathVariable Long id) {
        return ApiResponse.success(foodService.getFoodById(id));
    }

    /** 新增系统食物（进入公开食物库，所有用户可见） */
    @PostMapping
    public ApiResponse<Food> createFood(@Valid @RequestBody AddFoodRequest request) {
        return ApiResponse.success("新增成功", foodService.createSystemFood(request));
    }

    /** 编辑食物信息 */
    @PutMapping("/{id}")
    public ApiResponse<Food> updateFood(@PathVariable Long id, @Valid @RequestBody AddFoodRequest request) {
        return ApiResponse.success(foodService.updateFood(id, request));
    }

    /** 修改食物状态（审核通过 / 驳回 / 下架） */
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateFoodStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        foodService.updateFoodStatus(id, body.get("status"));
        return ApiResponse.success("更新成功", null);
    }

    /** 删除食物 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFood(@PathVariable Long id) {
        foodService.deleteFood(id);
        return ApiResponse.success("删除成功", null);
    }

    /** 获取用户提交的待审核食物列表 */
    @GetMapping("/pending")
    public ApiResponse<java.util.List<Food>> getPendingFoods() {
        return ApiResponse.success(foodService.getPendingFoods());
    }
}
