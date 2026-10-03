package com.zyyqq.dto.response;

import com.zyyqq.entity.Food;
import com.zyyqq.entity.UserCustomFood;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 食物选择项。
 * <p>
 * 把「系统食物」与「用户自定义食物」统一成同一种结构返回给前端，
 * 由 {@code custom} 标识来源：true = 当前用户私有的自定义食物，false = 公共食物库中的系统食物。
 * 前端提交饮食记录时需要把 {@code custom} 对应的来源回传（foodSource = user / system），
 * 后端据此在正确的表里解析食物。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodOptionVO {

    private Long id;

    private String name;

    private Long categoryId;

    private BigDecimal calories;

    private BigDecimal protein;

    private BigDecimal carbohydrate;

    private BigDecimal fat;

    private BigDecimal fiber;

    private String imageUrl;

    /** 是否为用户自定义（私有）食物 */
    private boolean custom;

    public static FoodOptionVO fromFood(Food food) {
        return FoodOptionVO.builder()
                .id(food.getId())
                .name(food.getName())
                .categoryId(food.getCategoryId())
                .calories(food.getCalories())
                .protein(food.getProtein())
                .carbohydrate(food.getCarbohydrate())
                .fat(food.getFat())
                .fiber(food.getFiber())
                .imageUrl(food.getImageUrl())
                .custom(false)
                .build();
    }

    public static FoodOptionVO fromCustom(UserCustomFood food) {
        return FoodOptionVO.builder()
                .id(food.getId())
                .name(food.getName())
                .categoryId(food.getCategoryId())
                .calories(food.getCalories())
                .protein(food.getProtein())
                .carbohydrate(food.getCarbohydrate())
                .fat(food.getFat())
                .fiber(food.getFiber())
                .custom(true)
                .build();
    }
}
