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

    List<Food> findByStatus(String status);

    List<Food> findBySourceAndStatus(String source, String status);

    @Query("SELECT f.id, f.name FROM Food f WHERE f.id IN :ids")
    List<Object[]> findIdAndNameByIds(@Param("ids") List<Long> ids);
}