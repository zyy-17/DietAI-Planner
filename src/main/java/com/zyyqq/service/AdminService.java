package com.zyyqq.service;

import com.zyyqq.entity.DietRecord;
import com.zyyqq.entity.User;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.AiGenerationLogRepository;
import com.zyyqq.repository.DietRecordRepository;
import com.zyyqq.repository.FoodRepository;
import com.zyyqq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端通用业务：数据看板统计、全局饮食记录管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    /** 系统食物来源标记（管理端只统计/管理这一类） */
    private static final String SOURCE_SYSTEM = "system";
    /** 用户自定义食物来源标记 */
    private static final String SOURCE_USER = "user";

    private final UserRepository userRepository;
    private final FoodRepository foodRepository;
    private final DietRecordRepository dietRecordRepository;
    private final AiGenerationLogRepository aiGenerationLogRepository;
    private final FoodService foodService;
    private final UserCustomFoodService userCustomFoodService;

    /** 数据看板核心指标 */
    public Map<String, Object> getOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalUsers", userRepository.countByDeleted(0));
        overview.put("todayNewUsers", userRepository.countByCreatedAtAfter(todayStart));
        // 食物库总数只统计系统食物，与"食物管理"列表口径一致
        overview.put("totalFoods", foodRepository.countBySource(SOURCE_SYSTEM));
        overview.put("totalDietRecords", dietRecordRepository.count());
        overview.put("todayDietRecords", dietRecordRepository.countByRecordDate(LocalDate.now()));
        overview.put("totalAiLogs", aiGenerationLogRepository.count());
        overview.put("abnormalAiLogs", aiGenerationLogRepository.countByIsAbnormal(1));
        return overview;
    }

    /**
     * 近 N 天的饮食记录趋势，缺失日期补 0，返回按日期升序的列表。
     * 每项结构：{ date: 'yyyy-MM-dd', count: n }
     */
    public List<Map<String, Object>> getDietRecordTrend(int days) {
        int span = Math.max(1, Math.min(days, 90));
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(span - 1L);

        Map<String, Long> countMap = new HashMap<>();
        for (Object[] row : dietRecordRepository.countGroupByDateBetween(start, end)) {
            LocalDate date = (LocalDate) row[0];
            countMap.put(date.toString(), ((Number) row[1]).longValue());
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", d.toString());
            item.put("count", countMap.getOrDefault(d.toString(), 0L));
            trend.add(item);
        }
        return trend;
    }

    /** 管理端分页查询饮食记录，可按用户、日期筛选，并填充食物名与用户名 */
    public Page<DietRecord> getDietRecords(Long userId, String date, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordDate"));
        LocalDate recordDate = parseDate(date);

        Page<DietRecord> records;
        if (userId != null && recordDate != null) {
            records = dietRecordRepository.findByUserIdAndRecordDateOrderByCreatedAtDesc(userId, recordDate, pageable);
        } else if (userId != null) {
            records = dietRecordRepository.findByUserIdOrderByRecordDateDescCreatedAtDesc(userId, pageable);
        } else if (recordDate != null) {
            records = dietRecordRepository.findByRecordDateOrderByCreatedAtDesc(recordDate, pageable);
        } else {
            records = dietRecordRepository.findAllByOrderByRecordDateDescCreatedAtDesc(pageable);
        }
        enrich(records.getContent());
        return records;
    }

    /** 管理端删除任意用户的饮食记录 */
    @Transactional
    public void deleteDietRecord(Long id) {
        if (!dietRecordRepository.existsById(id)) {
            throw new BusinessException("饮食记录不存在");
        }
        dietRecordRepository.deleteById(id);
        log.info("管理端删除饮食记录: id={}", id);
    }

    /** 填充食物名称与所属用户名，便于管理端列表展示 */
    private void enrich(List<DietRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        // 系统食物与用户自定义食物分属两张表，ID 各自独立，需按 food_source 分别查询
        List<Long> systemIds = new ArrayList<>();
        List<Long> customIds = new ArrayList<>();
        for (DietRecord record : records) {
            if (SOURCE_USER.equals(record.getFoodSource())) {
                customIds.add(record.getFoodId());
            } else {
                systemIds.add(record.getFoodId());
            }
        }
        Map<Long, String> systemNames = foodService.getFoodNamesByIds(systemIds.stream().distinct().toList());
        Map<Long, String> customNames = userCustomFoodService.getNamesByIds(customIds.stream().distinct().toList());

        List<Long> userIds = records.stream().map(DietRecord::getUserId).distinct().toList();
        Map<Long, String> userNameMap = new HashMap<>();
        for (User user : userRepository.findAllById(userIds)) {
            userNameMap.put(user.getId(), user.getUsername());
        }

        for (DietRecord record : records) {
            Map<Long, String> names = SOURCE_USER.equals(record.getFoodSource()) ? customNames : systemNames;
            record.setFoodName(names.getOrDefault(record.getFoodId(), "未知食物"));
            record.setUserName(userNameMap.getOrDefault(record.getUserId(), "未知用户"));
        }
    }

    /** 容错解析 yyyy-MM-dd，空值或非法值返回 null */
    private LocalDate parseDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("日期格式不正确，应为 yyyy-MM-dd");
        }
    }
}
