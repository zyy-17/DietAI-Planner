package com.zyyqq.repository;

import com.zyyqq.entity.MealPlanFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MealPlanFavoriteRepository extends JpaRepository<MealPlanFavorite, Long> {

    Optional<MealPlanFavorite> findByUserIdAndPlanId(Long userId, Long planId);

    /** 我收藏的食谱（按收藏时间倒序） */
    List<MealPlanFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 批量判断"哪些已收藏"，避免广场列表逐条查库 */
    List<MealPlanFavorite> findByUserIdAndPlanIdIn(Long userId, List<Long> planIds);

    long countByPlanId(Long planId);

    /** 食谱被删除时清掉收藏关系 */
    @Modifying
    @Query("delete from MealPlanFavorite f where f.planId = :planId")
    void deleteByPlanId(@Param("planId") Long planId);

    void deleteByUserId(Long userId);
}
