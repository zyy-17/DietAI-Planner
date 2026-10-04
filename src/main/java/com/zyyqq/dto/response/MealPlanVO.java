package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 食谱概要（卡片、列表与详情头部用）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanVO {

    private Long id;
    private String name;
    private String summary;
    private String goal;
    /** 目标中文名，如「减脂」 */
    private String goalLabel;
    private Integer days;
    /** 每日餐次（英文 code） */
    private List<String> meals;
    /** 每日目标热量 */
    private BigDecimal dailyCalories;
    /** 食谱内实际安排的平均每日热量 */
    private BigDecimal plannedDailyCalories;
    private LocalDate startDate;
    /** idle 未执行 / active 执行中 / archived 已归档 */
    private String status;
    /** 状态中文名，如「执行中」 */
    private String statusLabel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 今天是食谱的第几天（已按周期截断），未开始或已结束时用于定位 */
    private Integer currentDayIndex;
    private Integer itemCount;

    // ── 发布 / 广场 ───────────────────────────────────────────────

    private String coverUrl;
    /** 展示标签 */
    private List<String> tags;
    private String difficulty;
    private String expectedLoss;
    /** 心得 / 整体思路 */
    private String description;
    /** none / pending / approved / rejected */
    private String publishStatus;
    private String publishStatusLabel;
    /** 审核不通过的原因 */
    private String rejectReason;
    /** 被「保存为我的食谱」的次数 */
    private Integer usageCount;
    private Integer favoriteCount;
    private LocalDateTime publishedAt;
    /** 作者昵称（广场用，脱敏显示，如「用户**9」） */
    private String authorName;
    /** 是不是我自己的 */
    private boolean mine;
    /** 当前用户是否已收藏（广场用） */
    private boolean favorited;
    /** 是否已上架广场 */
    private boolean onSquare;
    /** 从广场复制而来时，指回原食谱 */
    private Long sourcePlanId;
}
