package com.zyyqq.repository;

import com.zyyqq.entity.FoodCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {

    List<FoodCategory> findByStatusOrderBySortOrderAsc(Integer status);

    List<FoodCategory> findByParentIdAndStatusOrderBySortOrderAsc(Long parentId, Integer status);

    default List<FoodCategory> findAllEnabled() {
        return findByStatusOrderBySortOrderAsc(1);
    }
}