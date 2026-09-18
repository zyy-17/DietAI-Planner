package com.zyyqq.repository;

import com.zyyqq.entity.Food;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    Page<Food> findByStatus(String status, Pageable pageable);

    Page<Food> findByCategoryIdAndStatus(Long categoryId, String status, Pageable pageable);

    @Query("SELECT f FROM Food f WHERE f.name LIKE %:keyword% AND f.status = :status")
    Page<Food> searchByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    @Query("SELECT f FROM Food f WHERE f.name LIKE %:keyword% AND f.status = 'approved'")
    List<Food> searchApproved(@Param("keyword") String keyword);

    /** 获取指定状态的所有食物列表 */
    List<Food> findByStatus(String status);

    /** 按来源和状态查询食物（如用户提交的待审核食物） */
    List<Food> findBySourceAndStatus(String source, String status);
}