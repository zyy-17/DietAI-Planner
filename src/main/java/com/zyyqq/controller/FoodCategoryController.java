package com.zyyqq.controller;

import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.entity.FoodCategory;
import com.zyyqq.service.FoodCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class FoodCategoryController {

    private final FoodCategoryService foodCategoryService;

    @GetMapping
    public ApiResponse<List<FoodCategory>> getAllCategories() {
        return ApiResponse.success(foodCategoryService.getAllCategories());
    }

    @GetMapping("/{parentId}/sub")
    public ApiResponse<List<FoodCategory>> getSubCategories(@PathVariable Long parentId) {
        return ApiResponse.success(foodCategoryService.getSubCategories(parentId));
    }
}