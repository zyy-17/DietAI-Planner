package com.zyyqq.repository;

import com.zyyqq.entity.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {

    /** 同一天一条，重复提交时据此覆盖 */
    Optional<WeightRecord> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);

    /** 区间内的记录按日期升序，折线图数据源 */
    List<WeightRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(Long userId, LocalDate from, LocalDate to);

    /** 按日期倒序，分页列表用（最新在前） */
    org.springframework.data.domain.Page<WeightRecord> findByUserIdOrderByRecordDateDesc(Long userId, org.springframework.data.domain.Pageable pageable);

    /** 最近一条，体重同步到 user 表时用 */
    Optional<WeightRecord> findFirstByUserIdOrderByRecordDateDesc(Long userId);

    /** 全部记录（删用户数据时清理用） */
    List<WeightRecord> findByUserId(Long userId);

    long countByUserId(Long userId);
}
