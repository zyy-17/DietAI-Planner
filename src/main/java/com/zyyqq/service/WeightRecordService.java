package com.zyyqq.service;

import com.zyyqq.dto.response.WeightTrendVO;
import com.zyyqq.entity.User;
import com.zyyqq.entity.WeightRecord;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.UserRepository;
import com.zyyqq.repository.WeightRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 体重/体脂记录。
 *
 * <p>同一用户同一天只有一条记录，重复提交按覆盖处理——这样折线图上不会出现同日多点。</p>
 */
@Service
@RequiredArgsConstructor
public class WeightRecordService {

    /** 健康体重上限，超过按体重秤量程的上限拒绝 */
    private static final BigDecimal MAX_WEIGHT = new BigDecimal("300");
    private static final BigDecimal MIN_WEIGHT = new BigDecimal("20");
    private static final BigDecimal MIN_BODY_FAT = BigDecimal.ZERO;
    private static final BigDecimal MAX_BODY_FAT = new BigDecimal("70");

    private final WeightRecordRepository repository;
    private final UserRepository userRepository;

    // ── 记录 ──────────────────────────────────────────────────────

    /**
     * 保存一条记录（同日期覆盖）。
     * 同时把该日期的体重同步到 user.weight，保证 BMR/TDEE 用的是最新数据。
     */
    @Transactional
    public WeightRecord save(Long userId, LocalDate date, BigDecimal weightKg,
                             BigDecimal bodyFatPercent, String remark) {
        validate(date, weightKg, bodyFatPercent);

        WeightRecord record = repository.findByUserIdAndRecordDate(userId, date)
                .orElseGet(() -> WeightRecord.builder().userId(userId).recordDate(date).build());

        record.setWeightKg(weightKg);
        record.setBodyFatPercent(bodyFatPercent);
        record.setRemark(remark);
        WeightRecord saved = repository.save(record);

        syncCurrentWeight(userId);
        return saved;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        WeightRecord record = repository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权删除他人的记录");
        }
        repository.delete(record);
        // 删掉的若是最新一条，user.weight 需回退到上一条
        syncCurrentWeight(userId);
    }

    /** 校验体重/体脂的合理区间与日期范围 */
    private void validate(LocalDate date, BigDecimal weightKg, BigDecimal bodyFatPercent) {
        if (date == null) throw new BusinessException("请选择记录日期");
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException("不能记录未来日期的体重");
        }
        if (weightKg == null) throw new BusinessException("请填写体重");
        if (weightKg.compareTo(MIN_WEIGHT) < 0 || weightKg.compareTo(MAX_WEIGHT) > 0) {
            throw new BusinessException("体重需在 " + MIN_WEIGHT + " ~ " + MAX_WEIGHT + " kg 之间");
        }
        if (bodyFatPercent != null) {
            if (bodyFatPercent.compareTo(MIN_BODY_FAT) < 0 || bodyFatPercent.compareTo(MAX_BODY_FAT) > 0) {
                throw new BusinessException("体脂率需在 0 ~ 70 % 之间");
            }
        }
    }

    /** 把最近一条记录的体重同步到 user 表（BMR/TDEE 依赖它） */
    private void syncCurrentWeight(Long userId) {
        Optional<WeightRecord> latest = repository.findFirstByUserIdOrderByRecordDateDesc(userId);
        Optional<User> userOpt = userRepository.findById(userId);
        if (latest.isPresent() && userOpt.isPresent()) {
            User user = userOpt.get();
            user.setWeight(latest.get().getWeightKg());
            userRepository.save(user);
        }
    }

    // ── 查询 ──────────────────────────────────────────────────────

    public Page<WeightRecord> page(Long userId, Pageable pageable) {
        return repository.findByUserIdOrderByRecordDateDesc(userId, pageable);
    }

    public List<WeightRecord> listRange(Long userId, LocalDate from, LocalDate to) {
        return repository.findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(userId, from, to);
    }

    /**
     * 趋势数据：折线图点 + 区间概览 + 目标进度。
     *
     * @param days 回看天数，30/90/180/365
     */
    @Transactional(readOnly = true)
    public WeightTrendVO trend(Long userId, int days) {
        int window = days <= 0 ? 90 : days;
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(window - 1L);

        List<WeightRecord> records = listRange(userId, from, today);
        List<WeightTrendVO.Point> points = new ArrayList<>(records.size());
        for (WeightRecord r : records) {
            points.add(WeightTrendVO.Point.builder()
                    .date(r.getRecordDate())
                    .weightKg(r.getWeightKg())
                    .bodyFatPercent(r.getBodyFatPercent())
                    .remark(r.getRemark())
                    .build());
        }

        return WeightTrendVO.builder()
                .points(points)
                .summary(buildSummary(records))
                .target(buildTarget(userId, records))
                .build();
    }

    private WeightTrendVO.Summary buildSummary(List<WeightRecord> records) {
        WeightTrendVO.Summary s = WeightTrendVO.Summary.builder().recordCount(records.size()).build();
        if (records.isEmpty()) return s;

        WeightRecord first = records.get(0);
        WeightRecord last = records.get(records.size() - 1);
        s.setStartWeight(first.getWeightKg());
        s.setLatestWeight(last.getWeightKg());
        s.setChangeKg(last.getWeightKg().subtract(first.getWeightKg()));
        s.setFirstDate(first.getRecordDate().toString());
        s.setLastDate(last.getRecordDate().toString());

        long days = ChronoUnit.DAYS.between(first.getRecordDate(), last.getRecordDate());
        if (days > 0) {
            // 折算成每周变化，让不同长度的区间可比
            BigDecimal perDay = s.getChangeKg().divide(BigDecimal.valueOf(days), 6, RoundingMode.HALF_UP);
            s.setAvgChangePerWeek(perDay.multiply(BigDecimal.valueOf(7)).setScale(2, RoundingMode.HALF_UP));
        } else {
            s.setAvgChangePerWeek(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        // 体脂：取最近一条有值的
        for (int i = records.size() - 1; i >= 0; i--) {
            if (records.get(i).getBodyFatPercent() != null) {
                s.setLatestBodyFat(records.get(i).getBodyFatPercent());
                for (int j = 0; j < i; j++) {
                    if (records.get(j).getBodyFatPercent() != null) {
                        s.setChangeBodyFat(records.get(i).getBodyFatPercent()
                                .subtract(records.get(j).getBodyFatPercent()));
                        break;
                    }
                }
                break;
            }
        }
        return s;
    }

    /**
     * 目标进度。
     *
     * <p>目标体脂优先于目标体重——若两者都设了，体脂更能代表真实进展；
     * 都没设则返回 null，前端不显示目标区块。</p>
     */
    private WeightTrendVO.Target buildTarget(Long userId, List<WeightRecord> records) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;
        if (user.getTargetWeightKg() == null && user.getTargetBodyFatPercent() == null) return null;

        boolean useFat = user.getTargetBodyFatPercent() != null;
        BigDecimal targetValue = useFat ? user.getTargetBodyFatPercent() : user.getTargetWeightKg();
        String unit = useFat ? "%" : "kg";

        if (records.isEmpty()) {
            return WeightTrendVO.Target.builder()
                    .targetWeightKg(user.getTargetWeightKg())
                    .targetBodyFatPercent(user.getTargetBodyFatPercent())
                    .targetDeadline(user.getTargetDeadline())
                    .dietGoal(user.getDietGoal())
                    .targetValue(targetValue)
                    .unit(unit)
                    .build();
        }

        BigDecimal startValue;
        BigDecimal currentValue;
        if (useFat) {
            // 起始值取最早一条有体脂的记录
            startValue = null;
            for (WeightRecord r : records) {
                if (r.getBodyFatPercent() != null) { startValue = r.getBodyFatPercent(); break; }
            }
            currentValue = null;
            for (int i = records.size() - 1; i >= 0; i--) {
                if (records.get(i).getBodyFatPercent() != null) { currentValue = records.get(i).getBodyFatPercent(); break; }
            }
            if (startValue == null || currentValue == null) {
                // 体脂数据不足，无法算进度，只回传目标本身
                return WeightTrendVO.Target.builder()
                        .targetWeightKg(user.getTargetWeightKg())
                        .targetBodyFatPercent(user.getTargetBodyFatPercent())
                        .targetDeadline(user.getTargetDeadline())
                        .dietGoal(user.getDietGoal())
                        .targetValue(targetValue)
                        .unit(unit)
                        .build();
            }
        } else {
            startValue = records.get(0).getWeightKg();
            currentValue = records.get(records.size() - 1).getWeightKg();
        }

        BigDecimal changed = currentValue.subtract(startValue);
        BigDecimal totalNeeded = targetValue.subtract(startValue);
        BigDecimal remaining = targetValue.subtract(currentValue);

        // 进度按"已完成 / 需要完成"算；分母为 0 说明起点就是目标方向之外，按 0 处理
        int progress = 0;
        if (totalNeeded.abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
            BigDecimal ratio = changed.divide(totalNeeded, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            progress = ratio.intValue();
            progress = Math.max(0, Math.min(100, progress));
        }

        // 达成日期估算：按近 7 天的平均速率
        WeightTrendVO.Summary summary = buildSummary(records);
        BigDecimal weekly = summary.getAvgChangePerWeek();
        String estimatedDate = null;
        Boolean tooFast = null;
        String advice = null;
        if (weekly != null && weekly.abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
            long days = Math.round(remaining.abs().divide(weekly.abs(), 0, RoundingMode.HALF_UP).longValue());
            estimatedDate = LocalDate.now().plusDays(days).toString();

            if (!useFat) {
                tooFast = isPaceTooFast(weekly, user.getDietGoal());
                if (tooFast) {
                    advice = "近期变化速度偏快（每周 " + weekly.abs().setScale(1, RoundingMode.HALF_UP)
                            + "kg），建议放缓，骤变容易反弹并影响健康";
                }
            }
        }

        return WeightTrendVO.Target.builder()
                .targetWeightKg(user.getTargetWeightKg())
                .targetBodyFatPercent(user.getTargetBodyFatPercent())
                .targetDeadline(user.getTargetDeadline())
                .dietGoal(user.getDietGoal())
                .startValue(startValue)
                .currentValue(currentValue)
                .targetValue(targetValue)
                .unit(unit)
                .remaining(remaining)
                .progressPercent(progress)
                .estimatedDate(estimatedDate)
                .paceTooFast(tooFast)
                .paceAdvice(advice)
                .build();
    }

    /** 安全速率：减重每周 0.5~1kg、增重每周 0.25~0.5kg 较为稳妥 */
    private boolean isPaceTooFast(BigDecimal weekly, String dietGoal) {
        BigDecimal abs = weekly.abs();
        if ("lose".equals(dietGoal)) return abs.compareTo(new BigDecimal("1.0")) > 0;
        if ("gain".equals(dietGoal)) return abs.compareTo(new BigDecimal("0.5")) > 0;
        return false;
    }
}
