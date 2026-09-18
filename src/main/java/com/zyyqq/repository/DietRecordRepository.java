package com.zyyqq.repository;

import com.zyyqq.entity.DietRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DietRecordRepository extends JpaRepository<DietRecord, Long> {

    List<DietRecord> findByUserIdAndRecordDateOrderByCreatedAtDesc(Long userId, LocalDate recordDate);

    List<DietRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateDescCreatedAtDesc(Long userId, LocalDate startDate, LocalDate endDate);

    /** 查询用户所有饮食记录，按日期倒序排列 */
    @Query("SELECT dr FROM DietRecord dr WHERE dr.userId = :userId ORDER BY dr.recordDate DESC, dr.createdAt DESC")
    List<DietRecord> findAllByUserIdOrderByDateDesc(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT dr.recordDate) FROM DietRecord dr WHERE dr.userId = :userId")
    long countDistinctRecordDatesByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(dr.calories) FROM DietRecord dr WHERE dr.userId = :userId")
    Double sumCaloriesByUserId(@Param("userId") Long userId);

    /** 根据ID和用户ID删除饮食记录（确保只能删除自己的记录） */
    void deleteByIdAndUserId(Long id, Long userId);
}