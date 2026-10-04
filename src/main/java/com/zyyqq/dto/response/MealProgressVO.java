package com.zyyqq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 今日某个餐次的执行状态：是否已经在饮食记录里出现。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealProgressVO {

    private String mealType;
    private String mealLabel;
    /** 今天这一餐有没有饮食记录 */
    private boolean done;
    /** 这一餐在饮食记录里的条数 */
    private int recordCount;
}
