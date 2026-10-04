package com.zyyqq.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 体重趋势统计：折线图数据 + 目标进度。
 */
@Data
@Builder
public class WeightTrendVO {

    /** 按日期升序的记录点 */
    private List<Point> points;

    private Summary summary;

    private Target target;

    @Data
    @Builder
    public static class Point {
        private LocalDate date;
        private BigDecimal weightKg;
        private BigDecimal bodyFatPercent;
        private String remark;
    }

    /** 区间内的统计概览 */
    @Data
    @Builder
    public static class Summary {
        private int recordCount;
        /** 区间内最新体重 */
        private BigDecimal latestWeight;
        /** 区间内最早体重 */
        private BigDecimal startWeight;
        /** 净变化（最新 - 最早），负数表示掉了 */
        private BigDecimal changeKg;
        /** 日均变化（正数表示在涨） */
        private BigDecimal avgChangePerWeek;
        private BigDecimal latestBodyFat;
        private BigDecimal changeBodyFat;
        private String firstDate;
        private String lastDate;
    }

    /** 目标与进度 */
    @Data
    @Builder
    public static class Target {
        private BigDecimal targetWeightKg;
        private BigDecimal targetBodyFatPercent;
        private LocalDate targetDeadline;
        private String dietGoal;

        /** 起始体重（按目标类型取体重或体脂的基准值） */
        private BigDecimal startValue;
        /** 当前值 */
        private BigDecimal currentValue;
        /** 目标值 */
        private BigDecimal targetValue;
        /** 单位：kg 或 % */
        private String unit;
        /** 距目标还差多少（正数表示还需变化多少） */
        private BigDecimal remaining;
        /** 已完成百分比 0~100 */
        private Integer progressPercent;
        /** 按近 7 天平均速率估算的达成日期 */
        private String estimatedDate;
        /** 速率是否过快（减重每周 > 1kg / 增重每周 > 0.5kg 视为过快） */
        private Boolean paceTooFast;
        private String paceAdvice;
    }
}
