package com.zyyqq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体重/体脂记录。
 *
 * <p>与 {@code diet_record}（饮食明细，按餐次多条）不同，这里是<b>每天最多一条</b>的
 * 身体数据快照：同一天重复提交会覆盖而非追加，这样图表上不会出现同日多点。</p>
 *
 * <p>体脂率设为可空——很多人家里没有体脂秤，硬填假数据反而污染趋势线，
 * 所以只记体重是完全合法的用法。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "weight_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_weight_user_date", columnNames = {"user_id", "record_date"}))
public class WeightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    /** 体重（kg），必填 */
    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 1)
    private BigDecimal weightKg;

    /** 体脂率（%），可选 */
    @Column(name = "body_fat_percent", precision = 5, scale = 1)
    private BigDecimal bodyFatPercent;

    /** 备注，如"早上空腹""刚运动完" */
    @Column(length = 200)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
