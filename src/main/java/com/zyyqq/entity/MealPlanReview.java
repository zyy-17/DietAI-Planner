package com.zyyqq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 食谱审核记录：每次通过 / 驳回都会落一条，保留审核人、时间、结果与原因。
 *
 * <p>与 {@code MealPlan.rejectReason} 不同，这里保留<b>完整历史</b>：一份食谱可能被
 * 驳回多次、每次原因不同，覆盖在单字段上会丢失之前的审核轨迹。管理员与作者都能
 * 据此追溯审核过程。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "meal_plan_review",
        indexes = @Index(name = "idx_mpr_plan", columnList = "plan_id"))
public class MealPlanReview {

    /** 审核结果：通过 */
    public static final String RESULT_APPROVED = "approved";
    /** 审核结果：驳回 */
    public static final String RESULT_REJECTED = "rejected";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    /** 审核人用户ID（管理员），系统自动审核时可为空 */
    @Column(name = "reviewer_id")
    private Long reviewerId;

    /** 审核人用户名快照，避免管理员改名/删号后显示空白 */
    @Column(name = "reviewer_name", length = 50)
    private String reviewerName;

    @Column(nullable = false, length = 20)
    private String result;

    @Column(length = 500)
    private String reason;

    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        this.reviewedAt = LocalDateTime.now();
    }
}
