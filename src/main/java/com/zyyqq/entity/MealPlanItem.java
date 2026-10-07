package com.zyyqq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 膳食方案里的一条食物条目（第几天 · 哪一餐 · 什么食物 · 多少克）。
 *
 * <p>营养值是<b>按克数换算后的快照</b>，不是每 100g 的值：方案的展示与汇总只读这张表，
 * 不必每次再回查食物库；同时食物名、单位也一起快照下来，这样管理员后续改名、
 * 甚至删掉某个食物，用户已经采用的方案也不会显示成空白。</p>
 *
 * <p>{@code foodId} 允许为空：AI 可能给出食物库里没有的食物，此时
 * {@code foodSource=ai}、{@code foodId=null}，营养值取 AI 的估算值。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "meal_plan_item")
public class MealPlanItem {

    /** 食物来源：食物库 */
    public static final String SOURCE_SYSTEM = "system";
    /** 食物来源：用户自定义食物 */
    public static final String SOURCE_USER = "user";
    /** 食物来源：AI 估算（食物库里没有这项） */
    public static final String SOURCE_AI = "ai";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    /** 第几天，从 1 开始 */
    @Column(name = "day_index", nullable = false)
    private Integer dayIndex;

    /** 餐次：breakfast / lunch / dinner / snack */
    @Column(name = "meal_type", nullable = false, length = 20)
    private String mealType;

    /** 食物库主键；AI 估算的食物为空 */
    @Column(name = "food_id")
    private Long foodId;

    @Column(name = "food_source", nullable = false, length = 20)
    @Builder.Default
    private String foodSource = SOURCE_SYSTEM;

    /** 食物名快照 */
    @Column(name = "food_name", nullable = false, length = 100)
    private String foodName;

    /** 食物图片快照：列表里要显示缩略图，快照下来省得每次回查食物库 */
    @Column(name = "image_url", length = 255)
    private String imageUrl;

    /** 克数 */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal amount;

    /** 常用单位名快照（个/份/盒…），为空表示只能按克展示 */
    @Column(name = "unit_name", length = 20)
    private String unitName;

    /** 每个/每份约多少克，用于把克数换算成"1个"这种更好读的写法 */
    @Column(name = "unit_weight", precision = 8, scale = 2)
    private BigDecimal unitWeight;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal calories;

    @Column(precision = 8, scale = 2)
    private BigDecimal protein;

    @Column(precision = 8, scale = 2)
    private BigDecimal carbohydrate;

    @Column(precision = 8, scale = 2)
    private BigDecimal fat;

    /** 同一餐内的展示顺序 */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
