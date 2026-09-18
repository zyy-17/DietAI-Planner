package com.zyyqq.controller.admin;

import com.zyyqq.dto.request.AddFoodRequest;
import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.entity.Food;
import com.zyyqq.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/foods")
@RequiredArgsConstructor
public class AdminFoodController {

    private final FoodService foodService;

    @GetMapping
    public ApiResponse<Page<Food>> getFoods(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(foodService.getAllFoodsForAdmin(status, page, size));
    }

    @PutMapping("/{id}")
    public ApiResponse<Food> updateFood(@PathVariable Long id, @RequestBody AddFoodRequest request) {
        return ApiResponse.success(foodService.updateFood(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateFoodStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        foodService.updateFoodStatus(id, body.get("status"));
        return ApiResponse.success("更新成功", null);
    }

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