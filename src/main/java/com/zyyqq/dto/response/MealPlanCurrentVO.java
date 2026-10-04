package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** 主页面「当前执行方案」卡：方案概要 + 今天吃什么 + 今日进度。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanCurrentVO {

    private MealPlanVO plan;
    /** 今天对应方案里的那一天（含菜品明细） */
    private MealPlanDayVO today;
    /** 今天是方案的第几天 */
    private Integer currentDayIndex;
    private LocalDate todayDate;
    /** 按餐次的执行进度，由今日饮食记录推导 */
    private List<MealProgressVO> progress;
    /** 专属饮食记录里今日总热量，用于对比方案安排 */
    private java.math.BigDecimal todayIntakeCalories;
}
