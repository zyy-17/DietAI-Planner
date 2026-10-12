package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 「我的食谱」列表页返回：状态计数 + 食谱列表。
 *
 * <p>状态计数按发布状态与执行状态两个维度拆开，方便前端在筛选栏直接展示数量：
 * 「全部 / 未发布 / 审核中 / 已发布 / 执行中」。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanListVO {

    /** 发布状态维度计数：key ∈ {none, pending, approved, rejected} */
    private Map<String, Long> publishCounts;

    /** 执行状态维度计数：key ∈ {idle, active, paused, archived} */
    private Map<String, Long> statusCounts;

    /** 总数（不含已软删除） */
    private long total;

    /** 当前筛选条件下的食谱列表 */
    private List<MealPlanVO> plans;
}
