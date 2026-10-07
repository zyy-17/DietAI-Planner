package com.zyyqq.repository;

import com.zyyqq.entity.UserCustomFood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCustomFoodRepository extends JpaRepository<UserCustomFood, Long> {

    /** 查询某用户的全部自定义食物（按创建时间倒序） */
    List<UserCustomFood> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 查询某用户创建的同名自定义食物（用于复用，避免重复创建） */
    Optional<UserCustomFood> findFirstByUserIdAndName(Long userId, String name);

    /** 按 ID + 用户ID 查询，确保只能访问本人的自定义食物 */
    Optional<UserCustomFood> findByIdAndUserId(Long id, Long userId);

    /** 批量查询 ID 与名称的映射，用于饮食记录展示 */
    @Query("SELECT u.id, u.name FROM UserCustomFood u WHERE u.id IN :ids")
    List<Object[]> findIdAndNameByIds(@Param("ids") List<Long> ids);
}
