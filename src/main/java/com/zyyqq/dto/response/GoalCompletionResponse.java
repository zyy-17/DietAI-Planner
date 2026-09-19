package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalCompletionResponse {

    private List<GoalItem> goals;
    private BigDecimal overallCompletion;
    private String overallStatus;
    private List<String> suggestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalItem {
        private String name;
        private BigDecimal target;
        private BigDecimal actual;
        private BigDecimal completion;
        private String status;
        private BigDecimal gap;
    }
}