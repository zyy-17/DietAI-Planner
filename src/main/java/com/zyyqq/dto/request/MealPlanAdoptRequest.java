package com.zyyqq.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 「采用此方案」的请求体。
 *
 * <p>结构刻意与 ai-service 返回的草稿保持一致（days[].meals.breakfast[].food/amount），
 * 前端把预览到的内容原样回传即可，不必自己重排成另一种格式；
 * 营养值不从这里接收——落库时按食物库重新精确换算，避免客户端传上来的数字被写进库。</p>
 */
@Data
public class MealPlanAdoptRequest {

    private String name;
    private String summary;
    private String goal;
    private BigDecimal dailyCalories;
    private List<String> meals;
    private List<String> preferences;
    private List<String> dislikes;
    private String extraRequirement;

    private List<Day> days;

    @Data
    public static class Day {
        private Integer day;
        private Map<String, List<Item>> meals;
    }

    @Data
    public static class Item {
        private String food;
        private BigDecimal amount;
    }
}
