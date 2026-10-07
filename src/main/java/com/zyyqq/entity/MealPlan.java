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
 * 食谱 / 膳食方案（多天计划）。
 *
 * <p>与 {@code diet_record}（用户"实际吃了什么"）不同，这里是用户"打算吃什么"。
 * 现在的用法是：<b>用户自己写食谱</b>（选食物、填克数），写完可以「应用」成执行中的方案
 * 逐天执行，也可以「发布」到食谱广场供别人收藏或保存成自己的。</p>
 *
 * <p>因此一份食谱有三个正交的状态：</p>
 * <ul>
 *   <li>{@code status} —— 执行状态：{@code idle} 只是躺在我的食谱里、{@code active} 正在执行
 *       （同一用户至多一份）、{@code archived} 执行过的历史</li>
 *   <li>{@code publishStatus} —— 发布状态：{@code none/pending/approved/rejected}，
 *       发布需管理员审核通过后才进广场</li>
 *   <li>{@code userId} —— 归属人；从广场「保存为我的食谱」会复制出一份新的，
 *       归属人变成保存者，{@code sourcePlanId} 指回原食谱</li>
 * </ul>
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

    /** 执行状态：躺在我的食谱里，未执行 */
    public static final String STATUS_IDLE = "idle";
    /** 执行状态：正在执行 */
    public static final String STATUS_ACTIVE = "active";
    /** 执行状态：已结束（历史） */
    public static final String STATUS_ARCHIVED = "archived";

    /** 发布状态：未发布（只有自己可见） */
    public static final String PUBLISH_NONE = "none";
    /** 发布状态：待管理员审核 */
    public static final String PUBLISH_PENDING = "pending";
    /** 发布状态：已上架到广场 */
    public static final String PUBLISH_APPROVED = "approved";
    /** 发布状态：审核未通过 */
    public static final String PUBLISH_REJECTED = "rejected";

    /** 广场排序：按人气（使用人数） */
    public static final String SORT_HOT = "hot";
    /** 广场排序：按最新上架 */
    public static final String SORT_NEW = "new";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 食谱名，如「7天健康减脂计划」 */
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

    /** 每日目标热量（kcal），可空——实际热量按食物库累计 */
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

    /** 一句话概述，卡片上展示 */
    @Column(length = 500)
    private String summary;

    /** 发布时的「心得 / 整体思路」，详情弹窗里可展开 */
    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_IDLE;

    // ── 发布相关 ───────────────────────────────────────────────────

    @Column(name = "publish_status", nullable = false, length = 20)
    @Builder.Default
    private String publishStatus = PUBLISH_NONE;

    /** 封面图地址（相对路径，如 /covers/xxx.jpg） */
    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    /** 展示标签，以「、」分隔，如「经典碳循环、力量训练」 */
    @Column(length = 200)
    private String tags;

    /** 难度：入门 / 进阶（自由文本） */
    @Column(length = 20)
    private String difficulty;

    /** 预期减重，如「-0.5~-1」或「-2kg」 */
    @Column(name = "expected_loss", length = 30)
    private String expectedLoss;

    /** 从广场复制而来时，指回原食谱 */
    @Column(name = "source_plan_id")
    private Long sourcePlanId;

    /** 被「保存为我的食谱」的次数 */
    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    /** 被收藏次数 */
    @Column(name = "favorite_count", nullable = false)
    @Builder.Default
    private Integer favoriteCount = 0;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /** 审核不通过的原因 */
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /** 方案开始日期，用于推算"今天是第几天" */
    @Column(name = "start_date")
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
        if (this.status == null) {
            this.status = STATUS_IDLE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** 是否已上架到广场 */
    public boolean isOnSquare() {
        return PUBLISH_APPROVED.equals(this.publishStatus);
    }
}
