package com.zyyqq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

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
 *   <li>{@code userId} —— 归属人</li>
 *   <li>{@code origin} —— 来源类型：{@code original} 用户原创（可完整编辑、可发布）；
 *       {@code copied} 应用自他人公开食谱（仅可替换/增删食材，不可改基础信息、不可二次发布）</li>
 * </ul>
 *
 * <p><b>版权保护</b>：{@code copied} 的食谱会同时记录 {@code sourcePlanId}（来源食谱 id）、
 * {@code sourceAuthorId}（原作者用户 id）与 {@code sourceAuthorName}（原作者昵称快照），
 * 用于来源溯源；服务层据此禁止修改基础信息与重新发布，避免把他人的原创食谱
 * 改头换面后当成自己的作品再次投稿。</p>
 *
 * <p>历史字段 {@code sourcePlanId} 保留：应用他人食谱时指回原食谱，
 * 是 {@code origin=copied} 的判定依据之一（老数据没有 origin 列，靠它兼容）。</p>
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
@SQLRestriction("deleted = false")
public class MealPlan {

    /** 执行状态：躺在我的食谱里，未执行 */
    public static final String STATUS_IDLE = "idle";
    /** 执行状态：正在执行 */
    public static final String STATUS_ACTIVE = "active";
    /** 执行状态：暂停（执行中途暂停，可恢复） */
    public static final String STATUS_PAUSED = "paused";
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

    /** 来源类型：用户原创，可完整编辑、可发布 */
    public static final String ORIGIN_ORIGINAL = "original";
    /** 来源类型：应用自他人公开食谱，仅可替换食材，不可改基础信息 / 不可二次发布 */
    public static final String ORIGIN_COPIED = "copied";

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

    /**
     * 来源类型：{@code original} 原创 / {@code copied} 应用自他人公开食谱。
     * 老数据可能为 null，配合 {@link #sourcePlanId} 一起判定（见 {@link #isCopied()}）。
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String origin = ORIGIN_ORIGINAL;

    /** 来源食谱原作者用户 ID（应用他人食谱时记录，用于溯源） */
    @Column(name = "source_author_id")
    private Long sourceAuthorId;

    /** 来源食谱原作者昵称快照（脱敏，如「用户**9」），原作者之后改名也不影响溯源展示 */
    @Column(name = "source_author_name", length = 100)
    private String sourceAuthorName;

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

    /** 软删除标记：被膳食方案/执行历史引用时不再物理删除，只打标记 */
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    /** 暂停时的第几天（恢复时从这天继续），无暂停历史时为 null */
    @Column(name = "paused_at_day")
    private Integer pausedAtDay;

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
        return PUBLISH_APPROVED.equals(this.publishStatus) && !isDeleted();
    }

    /** 是否已软删除 */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.deleted);
    }

    /**
     * 是否「应用自他人食谱」。
     *
     * <p>判定双保险：{@code origin=copied} 或存在 {@code sourcePlanId}（兼容 origin 列新增前的老数据）。</p>
     */
    public boolean isCopied() {
        return ORIGIN_COPIED.equals(this.origin) || this.sourcePlanId != null;
    }

    /** 是否用户原创（可完整编辑、可发布） */
    public boolean isOriginal() {
        return !isCopied();
    }
}
