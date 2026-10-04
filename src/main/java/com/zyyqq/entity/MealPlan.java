package com.zyyqq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 膳食方案（多天计划）。
 *
 * <p>与 {@code diet_record}（用户"实际吃了什么"）不同，这里是用户"打算吃什么"：
 * 由 AI 生成、用户确认后采用，随日期推进逐天执行。一个用户同时只有一份
 * {@code status=active} 的方案，旧方案在采用新方案时转为 {@code archived} 进历史。</p>
 *
 * <p>方案的「今日进度」不在这里存状态位，而是拿 {@code diet_record} 里今天的
 * 餐次记录反推——避免出现"方案说吃了、记录里没有"这种自相矛盾的数据。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "meal_plan")
public class MealPlan {

    /** 方案状态：执行中 */
    public static final String STATUS_ACTIVE = "active";
    /** 方案状态：已归档（历史方案） */
    public static final String STATUS_ARCHIVED = "archived";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 方案名，如「7天健康减脂计划」 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 膳食目标：lose 减脂 / gain 增重 / maintain 维持体重 */
    @Column(nullable = false, length = 20)
    private String goal;

    /** 方案周期（天）：3 / 7 / 14 */
    @Column(nullable = false)
    private Integer days;

    /** 每日餐次，逗号分隔：breakfast,lunch,dinner,snack */
    @Column(nullable = false, length = 100)
    private String meals;

    /** 每日目标热量（kcal） */
    @Column(name = "daily_calories", precision = 8, scale = 2)
    private BigDecimal dailyCalories;

    /** 饮食偏好标签，以「、」分隔 */
    @Column(length = 200)
    private String preferences;

    /** 不喜欢 / 不能吃的食物，以「、」分隔 */
    @Column(length = 500)
    private String dislikes;

    /** 其他要求（自由文本） */
    @Column(name = "extra_requirement", length = 500)
    private String extraRequirement;

    /** AI 给出的方案概述，卡片上展示一句话 */
    @Column(length = 500)
    private String summary;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_ACTIVE;

    /** 方案开始日期，用于推算"今天是第几天" */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.startDate == null) {
            this.startDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
