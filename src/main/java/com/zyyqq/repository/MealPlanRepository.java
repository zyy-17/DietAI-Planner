package com.zyyqq.repository;

import com.zyyqq.entity.MealPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    /** 我的食谱：按创建时间倒序 */
    List<MealPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 按执行状态取（active 用于"当前执行方案"，archived 用于历史） */
    List<MealPlan> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    /** 排除某个执行状态取全部（我的食谱列表：只要不是"已归档的历史"就都算我的食谱） */
    List<MealPlan> findByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, String status);

    /** 当前执行中的方案，同一用户至多一份 */
    Optional<MealPlan> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    /** 应用新方案时用来把旧的执行中方案批量转历史 */
    List<MealPlan> findByUserIdAndStatus(Long userId, String status);

    /** 删用户数据时清理用 */
    List<MealPlan> findByUserId(Long userId);

    long countByUserId(Long userId);

    // ── 食谱广场 ───────────────────────────────────────────────────

    /** 广场：按人气（使用人数）排序 */
    Page<MealPlan> findByPublishStatusOrderByUsageCountDescPublishedAtDescIdDesc(String publishStatus, Pageable pageable);

    /** 广场：按最新上架排序 */
    Page<MealPlan> findByPublishStatusOrderByPublishedAtDescIdDesc(String publishStatus, Pageable pageable);

    /** 广场：按名称模糊搜索（同样是已上架的） */
    Page<MealPlan> findByPublishStatusAndNameContainingOrderByUsageCountDesc(String publishStatus, String keyword, Pageable pageable);

    Page<MealPlan> findByPublishStatusAndNameContainingOrderByPublishedAtDesc(String publishStatus, String keyword, Pageable pageable);

    /** 广场详情：必须处于已上架状态 */
    Optional<MealPlan> findByIdAndPublishStatus(Long id, String publishStatus);

    List<MealPlan> findByIdInAndPublishStatus(List<Long> ids, String publishStatus);

    // ── 管理员审核 ─────────────────────────────────────────────────

    Page<MealPlan> findByPublishStatusOrderByUpdatedAtAscIdAsc(String publishStatus, Pageable pageable);

    long countByPublishStatus(String publishStatus);
}
