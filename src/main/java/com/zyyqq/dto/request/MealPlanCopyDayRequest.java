package com.zyyqq.dto.request;

import lombok.Data;

import java.util.List;

/** 「复制到其他日期」：把某一天整天的安排复制到另外几天。 */
@Data
public class MealPlanCopyDayRequest {

    /** 源日期（第几天） */
    private Integer fromDayIndex;

    /** 目标日期列表 */
    private List<Integer> toDayIndexes;

    /**
     * 目标日期上已有食物时是否覆盖。
     * false（默认）表示只补空位、不动已有的；true 表示先清空目标日期再复制。
     */
    private Boolean overwrite;
}
