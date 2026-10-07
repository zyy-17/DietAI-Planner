package com.zyyqq.repository;

import com.zyyqq.entity.DietRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // ==================== 管理端查询 ====================

    /** 管理端分页查询全部饮食记录（按日期倒序） */
    Page<DietRecord> findAllByOrderByRecordDateDescCreatedAtDesc(Pageable pageable);

    /** 管理端按用户分页查询饮食记录 */
    Page<DietRecord> findByUserIdOrderByRecordDateDescCreatedAtDesc(Long userId, Pageable pageable);

    /** 管理端按日期分页查询饮食记录 */
    Page<DietRecord> findByRecordDateOrderByCreatedAtDesc(LocalDate recordDate, Pageable pageable);

    /** 管理端按用户 + 日期分页查询饮食记录 */
    Page<DietRecord> findByUserIdAndRecordDateOrderByCreatedAtDesc(Long userId, LocalDate recordDate, Pageable pageable);

    /** 统计某天的饮食记录条数 */
    long countByRecordDate(LocalDate recordDate);

    /** 统计指定日期区间内每天的记录条数，用于趋势图 */
    @Query("SELECT dr.recordDate, COUNT(dr) FROM DietRecord dr WHERE dr.recordDate BETWEEN :start AND :end GROUP BY dr.recordDate ORDER BY dr.recordDate ASC")
    List<Object[]> countGroupByDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}