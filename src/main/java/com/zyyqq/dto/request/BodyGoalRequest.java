package com.zyyqq.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 设定身体目标。
 *
 * <p>三者都可为 null（表示"暂不设定"），但至少要给一个，否则没有可追踪的方向。</p>
 */
@Data
public class BodyGoalRequest {

    /** 目标体重（kg），减重/增重时填 */
    private BigDecimal targetWeightKg;

    /** 目标体脂率（%），可选；设了它则进度以体脂为准 */
    private BigDecimal targetBodyFatPercent;

    /** 期望达成日期，可选 */
    private LocalDate targetDeadline;
}
