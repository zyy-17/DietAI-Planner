package com.zyyqq.controller.admin;

import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.entity.FoodCategory;
import com.zyyqq.service.FoodCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final FoodCategoryService foodCategoryService;

    @PostMapping
    public ApiResponse<FoodCategory> createCategory(@RequestBody FoodCategory category) {
        return ApiResponse.success(foodCategoryService.createCategory(category));
    }

    @PutMapping("/{id}")
    public ApiResponse<FoodCategory> updateCategory(@PathVariable Long id, @RequestBody FoodCategory category) {
        return ApiResponse.success(foodCategoryService.updateCategory(id, category));
    }

    /** 删除食物分类 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        foodCategoryService.deleteCategory(id);
        return ApiResponse.success("删除成功", null);
    }
}