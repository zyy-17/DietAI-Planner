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
            @RequestParam(required = false) String foodType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (foodType != null && !foodType.isEmpty()) {
            return ApiResponse.success(foodService.getFoodsByType(foodType, page, size));
        }
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

    /** 新增用户自定义食物（仅创建，不记录饮食） */
    @PostMapping("/my")
    public ApiResponse<FoodOptionVO> createMyFood(Authentication authentication,
                                                  @RequestBody com.zyyqq.dto.request.UpdateCustomFoodRequest request) {
        Long userId = getUserIdOrNull(authentication);
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return ApiResponse.error("食物名称不能为空");
        }
        if (request.getCalories() == null) {
            return ApiResponse.error("热量不能为空");
        }
        var created = userCustomFoodService.findOrCreate(userId, request.getName(), request.getCategoryId(),
                request.getCalories(), request.getProtein(), request.getCarbohydrate(), request.getFat(),
                request.getFiber(), request.getUnitName(), request.getUnitWeight());
        return ApiResponse.success(FoodOptionVO.fromCustom(created));
    }

    /** 编辑当前用户的自定义食物（仅本人可改，组合食谱只可改名称/分类） */
    @PutMapping("/my/{id}")
    public ApiResponse<FoodOptionVO> updateMyFood(Authentication authentication, @PathVariable Long id,
                                                  @RequestBody com.zyyqq.dto.request.UpdateCustomFoodRequest request) {
        Long userId = getUserIdOrNull(authentication);
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        var updated = userCustomFoodService.update(id, userId, request.getName(), request.getCategoryId(),
                request.getCalories(), request.getProtein(), request.getCarbohydrate(), request.getFat(),
                request.getFiber(), request.getUnitName(), request.getUnitWeight());
        return ApiResponse.success(FoodOptionVO.fromCustom(updated));
    }

    /** 删除当前用户的自定义食物（仅本人可删） */
    @DeleteMapping("/my/{id}")
    public ApiResponse<Void> deleteMyFood(Authentication authentication, @PathVariable Long id) {
        Long userId = getUserIdOrNull(authentication);
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        userCustomFoodService.delete(id, userId);
        return ApiResponse.success("删除成功", null);
    }

    /** 创建/更新用户自定义食谱（由基础食材组合，营养自动累加） */
    @PostMapping("/my/recipe")
    public ApiResponse<FoodOptionVO> saveRecipe(Authentication authentication,
                                                @RequestBody com.zyyqq.dto.request.SaveCustomRecipeRequest request) {
        Long userId = getUserIdOrNull(authentication);
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        var recipe = userCustomFoodService.saveRecipe(userId, request);
        return ApiResponse.success(FoodOptionVO.fromCustom(recipe));
    }

    /** 查看某自定义食谱的组成明细 */
    @GetMapping("/my/recipe/{id}/items")
    public ApiResponse<java.util.List<com.zyyqq.entity.UserCustomFoodItem>> getRecipeItems(
            Authentication authentication, @PathVariable Long id) {
        Long userId = getUserIdOrNull(authentication);
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        return ApiResponse.success(userCustomFoodService.getRecipeItems(id, userId));
    }

    /** 从认证信息中提取用户ID，未登录或匿名访问时返回 null */
    private Long getUserIdOrNull(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }
}
