package com.zyyqq.controller.admin;

import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.entity.FoodCategory;
import com.zyyqq.repository.FoodCategoryRepository;
import com.zyyqq.service.FoodCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final FoodCategoryService foodCategoryService;
    private final FoodCategoryRepository foodCategoryRepository;

    /** 管理端查询全部分类（含已禁用），按排序值升序 */
    @GetMapping
    public ApiResponse<java.util.List<FoodCategory>> list() {
        return ApiResponse.success(foodCategoryRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")));
    }

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