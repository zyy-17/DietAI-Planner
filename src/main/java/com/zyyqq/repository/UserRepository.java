package com.zyyqq.repository;

import com.zyyqq.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}