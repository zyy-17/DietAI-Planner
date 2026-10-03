package com.zyyqq.repository;

import com.zyyqq.entity.Food;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    Page<Food> findByStatus(String status, Pageable pageable);

    Page<Food> findByCategoryIdAndStatus(Long categoryId, String status, Pageable pageable);

    @Query("SELECT f FROM Food f WHERE f.name LIKE %:keyword% AND f.status = :status")
    Page<Food> searchByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    @Query("SELECT f FROM Food f WHERE f.name LIKE %:keyword% AND f.status = 'approved'")
    List<Food> searchApproved(@Param("keyword") String keyword);

    List<Food> findByStatus(String status);

    List<Food> findBySourceAndStatus(String source, String status);

    // ==================== 公开食物库（不含任何用户自定义食物） ====================

    /** 分页查询公开食物（排除用户自定义来源，兼容 source 为空的旧数据） */
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

    // ==================== 用户自定义（私有）食物 ====================

    /** 查询某用户创建的自定义食物 */
    List<Food> findByCreatedByAndStatusAndSource(Long createdBy, String status, String source);

    /** 查询某用户创建的同名自定义食物（用于复用，避免重复创建） */
    Optional<Food> findFirstByCreatedByAndNameAndSource(Long createdBy, String name, String source);

    @Query("SELECT f.id, f.name FROM Food f WHERE f.id IN :ids")
    List<Object[]> findIdAndNameByIds(@Param("ids") List<Long> ids);

    // ==================== 管理端统计 ====================

    /** 统计指定状态的食物数量 */
    long countByStatus(String status);

    /** 管理端按关键字（名称）模糊分页查询全部食物 */
    @Query("SELECT f FROM Food f WHERE f.name LIKE %:keyword% ORDER BY f.createdAt DESC")
    Page<Food> searchAllByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /** 管理端按来源分页查询食物 */
    Page<Food> findBySource(String source, Pageable pageable);
}
