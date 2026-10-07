package com.zyyqq.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddDietRecordRequest {

    @NotNull(message = "食物ID不能为空")
    private Long foodId;

    /**
     * 食物来源：system = 公共食物库（默认），user = 用户自定义食物。
     * 用户在前端选择了自己添加的自定义食物时需传 user。
     */
    private String foodSource;

    @NotNull(message = "餐次不能为空")
    private String mealType;

    @NotNull(message = "食用份量不能为空")
    private BigDecimal amount;
}