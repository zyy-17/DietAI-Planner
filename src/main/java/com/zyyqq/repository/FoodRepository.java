package com.zyyqq.repository;

import com.zyyqq.entity.Food;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 公共食物库仓储。
 * <p>
 * food 表只存放系统食物（source='system'）；用户自定义食物在 user_custom_food 表，
 * 见 {@link UserCustomFoodRepository}。以下公开食物查询仍保留来源过滤，
 * 用于兼容历史上可能残留的 source='user' 数据。
 */
@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    // ==================== 公开食物库 ====================

    /** 分页查询公开食物（排除用户自定义来源） */
    @Query("SELECT f FROM Food f WHERE f.status = :status AND (f.source IS NULL OR f.source <> :source)")
    Page<Food> findPublicFoods(@Param("status") String status, @Param("source") String source, Pageable pageable);

    /** 按分类分页查询公开食物 */
    @Query("SELECT f FROM Food f WHERE f.categoryId = :categoryId AND f.status = :status AND (f.source IS NULL OR f.source <> :source)")
    Page<Food> findPublicFoodsByCategory(@Param("categoryId") Long categoryId, @Param("status") String status,
                                         @Param("source") String source, Pageable pageable);

    /** 按关键字分页查询公开食物 */
    @Query("SELECT f FROM Food f WHERE f.name LIKE %:keyword% AND f.status = :status AND (f.source IS NULL OR f.source <> :source)")
    Page<Food> searchPublicFoods(@Param("keyword") String keyword, @Param("status") String status,
                                 @Param("source") String source, Pageable pageable);

    /** 查询全部公开食物 */
    @Query("SELECT f FROM Food f WHERE f.status = :status AND (f.source IS NULL OR f.source <> :source)")
    List<Food> findPublicFoodList(@Param("status") String status, @Param("source") String source);

    /** 按关键字搜索公开食物 */
    @Query("SELECT f FROM Food f WHERE f.name LIKE %:keyword% AND f.status = 'approved' AND (f.source IS NULL OR f.source <> 'user')")
    List<Food> searchPublicApproved(@Param("keyword") String keyword);

    /** 批量查询 ID 与名称的映射，用于饮食记录展示 */
    @Query("SELECT f.id, f.name FROM Food f WHERE f.id IN :ids")
    List<Object[]> findIdAndNameByIds(@Param("ids") List<Long> ids);

    // ==================== 管理端食物库（仅系统食物） ====================

    /** 统计指定来源的食物数量 */
    long countBySource(String source);

    /** 管理端分页查询系统食物 */
    @Query("SELECT f FROM Food f WHERE f.source = :source")
    Page<Food> findAdminFoods(@Param("source") String source, Pageable pageable);

    /** 管理端按状态分页查询系统食物 */
    @Query("SELECT f FROM Food f WHERE f.source = :source AND f.status = :status")
    Page<Food> findAdminFoodsByStatus(@Param("source") String source, @Param("status") String status, Pageable pageable);

    /** 管理端按名称关键字分页查询系统食物 */
    @Query("SELECT f FROM Food f WHERE f.source = :source AND f.name LIKE %:keyword%")
    Page<Food> searchAdminFoods(@Param("source") String source, @Param("keyword") String keyword, Pageable pageable);

    /** 管理端按状态 + 名称关键字分页查询系统食物 */
    @Query("SELECT f FROM Food f WHERE f.source = :source AND f.status = :status AND f.name LIKE %:keyword%")
    Page<Food> searchAdminFoodsByStatus(@Param("source") String source, @Param("status") String status,
                                        @Param("keyword") String keyword, Pageable pageable);
}
