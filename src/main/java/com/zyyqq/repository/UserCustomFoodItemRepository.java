package com.zyyqq.repository;

import com.zyyqq.entity.UserCustomFoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户自定义食谱组成明细仓储。
 */
@Repository
public interface UserCustomFoodItemRepository extends JpaRepository<UserCustomFoodItem, Long> {

    /** 某食谱的全部组成明细（按创建顺序） */
    List<UserCustomFoodItem> findByRecipeIdOrderByIdAsc(Long recipeId);

    /** 删除某食谱的全部明细（重建食谱时先清空再写） */
    void deleteByRecipeId(Long recipeId);
}
