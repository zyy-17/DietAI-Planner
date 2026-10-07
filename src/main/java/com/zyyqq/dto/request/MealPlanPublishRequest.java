package com.zyyqq.dto.request;

import lombok.Data;

import java.util.List;

/** 提交发布到食谱广场（提交后进入待审核，管理员通过才上架）。 */
@Data
public class MealPlanPublishRequest {

    /** 封面图地址（先调封面上传接口拿到） */
    private String coverUrl;

    /** 展示标签，如「经典碳循环」「力量训练」 */
    private List<String> tags;

    /** 难度：入门 / 进阶 */
    private String difficulty;

    /** 预期减重，如「-0.5~-1」 */
    private String expectedLoss;

    /** 心得 / 整体思路，详情里可展开 */
    private String description;

    /** 一句话概述 */
    private String summary;
}
