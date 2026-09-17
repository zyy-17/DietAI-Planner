package com.zyyqq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddFoodRequest {

    @NotBlank(message = "食物名称不能为空")
    private String name;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotNull(message = "热量不能为空")
    private BigDecimal calories;

    private BigDecimal protein;

    private BigDecimal carbohydrate;

    private BigDecimal fat;

    private BigDecimal fiber;

    private String imageUrl;
}