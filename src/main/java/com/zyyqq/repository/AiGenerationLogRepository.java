package com.zyyqq.repository;

import com.zyyqq.entity.AiGenerationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, Long> {

    Page<AiGenerationLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AiGenerationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AiGenerationLog> findByTypeOrderByCreatedAtDesc(String type, Pageable pageable);
}