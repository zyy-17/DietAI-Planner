package com.zyyqq.controller;

import com.zyyqq.dto.request.AddFoodRequest;
import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.entity.Food;
import com.zyyqq.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping
    public ApiResponse<Page<Food>> getFoods(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(foodService.getFoods(categoryId, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Food> getFoodById(@PathVariable Long id) {
        return ApiResponse.success(foodService.getFoodById(id));
    }

    @GetMapping("/search")
    public ApiResponse<java.util.List<Food>> searchFoods(@RequestParam String keyword) {
        return ApiResponse.success(foodService.searchFoods(keyword));
    }

    @PostMapping
    public ApiResponse<Food> addFood(Authentication authentication,
                                     @RequestBody AddFoodRequest request) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(foodService.addFoodByUser(request, userId));
    }

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}