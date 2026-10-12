package com.zyyqq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户自定义食谱（组合食物）的一条组成明细。
 * <p>
 * 当用户把「若干基础食材」拼成一道常吃的菜并保存为个人食谱时，
 * 这道食谱本身存在 {@link UserCustomFood}（isRecipe=true，营养值为累加后的总量），
 * 本表记录它由哪些食材、各多少克组成，便于后续查看/编辑明细。
 * </p>
 * <p>营养值按克数换算后的快照（每 100g 值 × 克数 ÷ 100），只读展示用，
 * 食谱的营养总量在创建/更新时统一重新累加，避免两处数据不一致。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_custom_food_item")
public class UserCustomFoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属食谱（指向 user_custom_food.id，且 isRecipe=true） */
    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    /** 组成食材的 food 表主键（基础食材） */
    @Column(name = "food_id", nullable = false)
    private Long foodId;

    /** 食材名快照（避免食材后续改名/删除后明细显示空白） */
    @Column(name = "food_name", nullable = false, length = 100)
    private String foodName;

    /** 食材用量（克） */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal amount;

    /** 该食材贡献的热量快照（kcal） */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal calories;

    /** 该食材贡献的蛋白质快照（g） */
    @Column(precision = 8, scale = 2)
    private BigDecimal protein;

    /** 该食材贡献的碳水快照（g） */
    @Column(precision = 8, scale = 2)
    private BigDecimal carbohydrate;

    /** 该食材贡献的脂肪快照（g） */
    @Column(precision = 8, scale = 2)
    private BigDecimal fat;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
