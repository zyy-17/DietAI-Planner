package com.zyyqq.repository;

import com.zyyqq.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    /** 历史方案列表：按创建时间倒序 */
    List<MealPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 按状态取方案（active 用于"当前执行方案"，archived 用于历史） */
    List<MealPlan> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    /** 当前执行中的方案，同一用户至多一份 */
    Optional<MealPlan> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    /** 采用新方案时用来把旧的执行中方案批量转历史 */
    List<MealPlan> findByUserIdAndStatus(Long userId, String status);

    /** 删用户数据时清理用 */
    List<MealPlan> findByUserId(Long userId);

    long countByUserId(Long userId);
}
