package com.zyyqq.dto.request;

import lombok.Data;

/** 管理员驳回食谱时填写的原因。 */
@Data
public class MealPlanRejectRequest {

    private String reason;
}
