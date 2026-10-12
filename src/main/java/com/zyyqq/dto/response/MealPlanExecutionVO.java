package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 食谱执行闭环的进度视图：周期进度、每日打卡状态、计划 vs 实际营养差异。
 *
 * <p>「打卡」不引入独立打卡表，而是从 {@code diet_record} 反推：某天记录了几餐就标几餐
 * 完成，与「今日进度」同一套口径，避免两套账。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanExecutionVO {

    private MealPlanVO plan;
    /** 开始执行日期 */
    private LocalDate startDate;
    /** 今天是第几天（按周期截断） */
    private Integer currentDayIndex;
    /** 周期总天数 */
    private Integer totalDays;
    /** 每天开启的餐次（英文 code） */
    private List<String> meals;
    /** 已过天数（含今天），用于算进度百分比 */
    private Integer elapsedDays;
    /** 已完成打卡的「天·餐」总数 */
    private Integer doneMeals;
    /** 周期内已开启的总「天·餐」数 */
    private Integer totalMeals;
    /** 完成百分比（0-100，整数） */
    private Integer progressPercent;
    /** 每日打卡状态（只含已过日期） */
    private List<MealPlanDayProgressVO> days;
    /** 计划平均每日热量（按食谱安排） */
    private BigDecimal plannedDailyCalories;
    /** 已过日期实际摄入的日均热量 */
    private BigDecimal actualDailyCalories;
    /** 计划 vs 实际热量差（planned - actual，正数=吃少了） */
    private BigDecimal calorieGap;
}
