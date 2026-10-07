package com.zyyqq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column
    private Integer gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(precision = 5, scale = 1)
    private BigDecimal height;

    @Column(precision = 5, scale = 1)
    private BigDecimal weight;

    /**
     * 目标体重（kg）。减重/增重时设定后，目标热量会结合它与当前体重的差距来计算，
     * 比固定系数准得多。为空则沿用按 diet_goal 取 0.8/1.15 系数的旧算法。
     */
    @Column(name = "target_weight_kg", precision = 5, scale = 1)
    private BigDecimal targetWeightKg;

    /** 目标体脂率（%），可选。体脂是比体重更准确的努力方向，但多数人测不了 */
    @Column(name = "target_body_fat_percent", precision = 5, scale = 1)
    private BigDecimal targetBodyFatPercent;

    /** 期望达成目标日期，用于估算"还需多久"并提示是否过快 */
    @Column(name = "target_deadline")
    private java.time.LocalDate targetDeadline;

    @Column(name = "activity_level")
    private Integer activityLevel;

    @Column(name = "diet_goal", length = 20)
    private String dietGoal;

    @Column(name = "diet_preference", length = 200)
    private String dietPreference;

    /** 忌口食物，多个以「、」分隔，取值来自 profile_option 表 allergy 分组 */
    @Column(name = "allergy_note", length = 200)
    private String allergyNote;

    /** 慢性疾病，多个以「、」分隔，取值来自 profile_option 表 disease 分组 */
    @Column(name = "disease", length = 200)
    private String disease;

    /** 用药情况，自由文本 */
    @Column(name = "medication", length = 500)
    private String medication;

    @Column(name = "target_calories", precision = 7, scale = 2)
    private BigDecimal targetCalories;

    @Column(name = "target_protein", precision = 7, scale = 2)
    private BigDecimal targetProtein;

    @Column(name = "target_carbohydrate", precision = 7, scale = 2)
    private BigDecimal targetCarbohydrate;

    @Column(name = "target_fat", precision = 7, scale = 2)
    private BigDecimal targetFat;

    @Column(name = "diet_reminder")
    @Builder.Default
    private Boolean dietReminder = true;

    @Column(name = "reminder_time", length = 5)
    @Builder.Default
    private String reminderTime = "08:00";

    @Column(name = "goal_reminder")
    @Builder.Default
    private Boolean goalReminder = true;

    @Column(name = "ai_suggestion")
    @Builder.Default
    private Boolean aiSuggestion = true;

    @Column(name = "theme", length = 10)
    @Builder.Default
    private String theme = "light";

    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "zh-CN";

    @Column(name = "collapsed_sidebar")
    @Builder.Default
    private Boolean collapsedSidebar = false;

    @Column(name = "data_sharing")
    @Builder.Default
    private Boolean dataSharing = false;

    @Column(name = "public_records")
    @Builder.Default
    private Boolean publicRecords = false;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "user";

    @Column(nullable = false)
    @Builder.Default
    private Integer status = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Integer deleted = 0;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.email != null && this.email.isEmpty()) this.email = null;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.email != null && this.email.isEmpty()) this.email = null;
    }
}