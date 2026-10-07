package com.zyyqq.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增/更新体重体脂记录。体重必填，体脂可选。
 */
@Data
public class WeightRecordRequest {

    /** 记录日期，不传默认今天 */
    private LocalDate recordDate;

    private BigDecimal weightKg;

    private BigDecimal bodyFatPercent;

    private String remark;
}
