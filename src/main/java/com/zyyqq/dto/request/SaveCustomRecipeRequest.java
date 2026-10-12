package com.zyyqq.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建/更新「用户自定义食谱」请求。
 * <p>
 * 食谱由若干基础食材组合而成，营养值由后端按各食材每 100g 值 × 克数累加，
 * 用户无需手填营养数据，实现「精准 + 快捷」兼得。
 * </p>
 */
@Data
public class SaveCustomRecipeRequest {

    /** 食谱名，如「我的家常蛋炒饭」 */
    @NotBlank(message = "食谱名称不能为空")
    private String name;

    /** 可选，未指定时归入"其他"分类 */
    private Long categoryId;

    /** 组成食材列表（至少 1 项） */
    @NotEmpty(message = "至少添加一种食材")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        /** 基础食材的 food 表主键 */
        @NotNull(message = "食材不能为空")
        private Long foodId;

        /** 食材用量（克） */
        @NotNull(message = "食材用量不能为空")
        private BigDecimal amount;
    }
}
