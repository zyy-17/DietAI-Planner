package com.zyyqq.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    private String realName;

    private Integer gender;

    private LocalDate birthDate;

    private BigDecimal height;

    private BigDecimal weight;

    private Integer activityLevel;

    private String dietGoal;

    private String dietPreference;

    /** 忌口食物，多个以「、」分隔 */
    private String allergyNote;

    /** 慢性疾病，多个以「、」分隔 */
    private String disease;

    /** 用药情况，自由文本 */
    private String medication;

    private String avatarUrl;
}