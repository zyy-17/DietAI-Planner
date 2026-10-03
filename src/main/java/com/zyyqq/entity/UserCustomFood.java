package com.zyyqq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户自定义食物。
 * <p>
 * 独立于公共食物库（food 表）存储，仅创建者本人可见、可用，
 * 不会出现在食物库列表、食物搜索与食物推荐中。
 * 营养数值均为每 100g 的含量。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_custom_food")
public class UserCustomFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal calories;

    @Column(precision = 8, scale = 2)
    private BigDecimal protein;

    @Column(precision = 8, scale = 2)
    private BigDecimal carbohydrate;

    @Column(precision = 8, scale = 2)
    private BigDecimal fat;

    @Column(precision = 8, scale = 2)
    private BigDecimal fiber;

    /** 常用计量单位名称（个/份/盒/片…），为空表示只能按克数记录 */
    @Column(name = "unit_name", length = 20)
    private String unitName;

    /** 每个/每份约多少克，配合 unitName 做「按个数」换算 */
    @Column(name = "unit_weight", precision = 8, scale = 2)
    private BigDecimal unitWeight;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
