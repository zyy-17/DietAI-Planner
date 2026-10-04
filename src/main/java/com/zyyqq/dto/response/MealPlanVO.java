package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 方案概要（卡片与列表用）。 */
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
    /** 方案内实际安排的平均每日热量 */
    private BigDecimal plannedDailyCalories;
    private LocalDate startDate;
    /** active 执行中 / archived 历史 */
    private String status;
    private LocalDateTime createdAt;
    /** 今天是方案的第几天（已按周期截断），未开始或已结束时用于定位 */
    private Integer currentDayIndex;
    private Integer itemCount;
}
