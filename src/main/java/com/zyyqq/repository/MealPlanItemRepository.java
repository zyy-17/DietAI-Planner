package com.zyyqq.repository;

import com.zyyqq.entity.MealPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MealPlanItemRepository extends JpaRepository<MealPlanItem, Long> {

    /** 整个方案的所有条目：先按天、再按餐次内顺序 */
    List<MealPlanItem> findByPlanIdOrderByDayIndexAscSortOrderAscIdAsc(Long planId);

    /** 指定某一天的条目，详情页切换日期时用 */
    List<MealPlanItem> findByPlanIdAndDayIndexOrderBySortOrderAscIdAsc(Long planId, Integer dayIndex);

    /** 多个方案一次取回条目，避免列表页 N+1 查询 */
    List<MealPlanItem> findByPlanIdInOrderByDayIndexAscSortOrderAscIdAsc(List<Long> planIds);

    long countByPlanId(Long planId);

    @Modifying
    @Query("delete from MealPlanItem i where i.planId = :planId")
    void deleteByPlanId(@Param("planId") Long planId);
}
