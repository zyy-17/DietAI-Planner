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
}