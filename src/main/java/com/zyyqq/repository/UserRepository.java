package com.zyyqq.repository;

import com.zyyqq.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndDeleted(String username, Integer deleted);

    Optional<User> findByEmailAndDeleted(String email, Integer deleted);

    default Optional<User> findByUsernameActive(String username) {
        return findByUsernameAndDeleted(username, 0);
    }

    default Optional<User> findByEmailActive(String email) {
        return findByEmailAndDeleted(email, 0);
    }

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}