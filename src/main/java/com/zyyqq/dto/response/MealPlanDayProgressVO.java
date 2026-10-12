package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 执行周期里某一天的打卡状态（由饮食记录反推）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanDayProgressVO {

    /** 该日期的第几天（从 1 开始） */
    private Integer dayIndex;
    /** 实际日期 */
    private LocalDate date;
    /** 当天计划热量（食谱安排） */
    private BigDecimal plannedCalories;
    /** 当天实际摄入热量（饮食记录） */
    private BigDecimal actualCalories;
    /** 当天完成打卡的餐次数 */
    private Integer doneMeals;
    /** 当天开启的总餐次数 */
    private Integer totalMeals;
    /** 当天是否全部餐次都有记录 */
    private boolean completed;
    /** 按餐次的完成情况 */
    private List<MealProgressVO> meals;
}
