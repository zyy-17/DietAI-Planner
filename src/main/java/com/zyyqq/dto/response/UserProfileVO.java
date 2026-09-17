package com.zyyqq.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileVO {

    private Long id;
    private String username;
    private String email;
    private String realName;
    private Integer gender;
    private LocalDate birthDate;
    private BigDecimal height;
    private BigDecimal weight;
    private Integer activityLevel;
    private String dietGoal;
    private String dietPreference;
    private String avatarUrl;
    private String role;
    private LocalDateTime createdAt;
}