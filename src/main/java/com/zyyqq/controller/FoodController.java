package com.zyyqq.controller;

import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.dto.response.FoodOptionVO;
import com.zyyqq.entity.Food;
import com.zyyqq.service.FoodService;
import com.zyyqq.service.UserCustomFoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;
    private final UserCustomFoodService userCustomFoodService;

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

    /**
     * 获取当前用户可选择的食物列表：公共食物库 + 本人自定义食物。
     * 每项的 custom 字段标识是否为本人私有食物（自定义食物存放在 user_custom_food 表，不进入公共食物库）。
     */
    @GetMapping("/all")
    public ApiResponse<java.util.List<FoodOptionVO>> getAllFoods(Authentication authentication) {
        return ApiResponse.success(foodService.getAvailableFoodOptions(getUserIdOrNull(authentication)));
    }

    /** 获取当前用户自己添加的自定义食物（仅本人可见） */
    @GetMapping("/my")
    public ApiResponse<java.util.List<FoodOptionVO>> getMyFoods(Authentication authentication) {
        return ApiResponse.success(userCustomFoodService.listMineAsOptions(getUserIdOrNull(authentication)));
    }

    /** 从认证信息中提取用户ID，未登录或匿名访问时返回 null */
    private Long getUserIdOrNull(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }
}
