package com.zyyqq.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddDietRecordRequest {

    @NotNull(message = "食物ID不能为空")
    private Long foodId;

    @NotNull(message = "餐次不能为空")
    private String mealType;

    @NotNull(message = "食用份量不能为空")
    private BigDecimal amount;
}