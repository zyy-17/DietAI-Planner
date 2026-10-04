package com.zyyqq.repository;

import com.zyyqq.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndDeleted(String username, Integer deleted);

    Optional<User> findByEmailAndDeleted(String email, Integer deleted);

    /** 查询未删除的活跃用户（按用户名） */
    default Optional<User> findByUsernameActive(String username) {
        return findByUsernameAndDeleted(username, 0);
    }

    default Optional<User> findByEmailActive(String email) {
        return findByEmailAndDeleted(email, 0);
    }

    boolean existsByUsername(String username);

    /** 检查邮箱是否已注册 */
    boolean existsByEmail(String email);

    // ==================== 管理端查询 ====================

    /** 管理端分页查询未删除用户（按注册时间倒序） */
    Page<User> findByDeletedOrderByCreatedAtDesc(Integer deleted, Pageable pageable);

    /** 管理端按关键字（用户名/邮箱/真实姓名）模糊分页查询未删除用户 */
    @Query("SELECT u FROM User u WHERE u.deleted = 0 AND (u.username LIKE %:keyword% OR u.email LIKE %:keyword% OR u.realName LIKE %:keyword%) ORDER BY u.createdAt DESC")
    Page<User> searchActiveUsers(@Param("keyword") String keyword, Pageable pageable);

    /** 统计未删除用户数量 */
    long countByDeleted(Integer deleted);

    /** 统计指定时间之后注册的用户数量（用于今日新增） */
    long countByCreatedAtAfter(LocalDateTime time);

    // ==================== 档案选项引用统计 ====================
    // 用于判断某个选项能否删除：已有用户使用时禁止物理删除，引导改为停用

    List<User> findByDietGoal(String dietGoal);

    List<User> findByActivityLevel(Integer activityLevel);

    List<User> findByDietPreferenceIsNotNull();

    List<User> findByAllergyNoteIsNotNull();

    List<User> findByDiseaseIsNotNull();
}