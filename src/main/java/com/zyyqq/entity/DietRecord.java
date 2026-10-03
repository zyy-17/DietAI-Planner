package com.zyyqq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "diet_record")
public class DietRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "food_id", nullable = false)
    private Long foodId;

    /**
     * 食物来源：system = 公共食物库（food 表），user = 用户自定义食物（user_custom_food 表）。
     * 两张表的 ID 各自独立，必须靠该字段才能正确解析食物名称。
     */
    @Column(name = "food_source", nullable = false, length = 20)
    @Builder.Default
    private String foodSource = "system";

    @Column(name = "meal_type", nullable = false, length = 20)
    private String mealType;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal calories;

    @Column(precision = 8, scale = 2)
    private BigDecimal protein;

    @Column(precision = 8, scale = 2)
    private BigDecimal carbohydrate;

    @Column(precision = 8, scale = 2)
    private BigDecimal fat;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private String foodName;

    /** 管理端展示用：所属用户名（非数据库字段） */
    @Transient
    private String userName;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}