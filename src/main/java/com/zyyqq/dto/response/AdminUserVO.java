package com.zyyqq.dto.response;

import com.zyyqq.entity.User;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理端用户列表/详情视图对象，剔除密码等敏感字段。
 */
@Data
@Builder
public class AdminUserVO {

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
    private String role;
    private Integer status;
    private LocalDateTime createdAt;

    /** 由用户实体转换，不包含密码哈希 */
    public static AdminUserVO from(User user) {
        return AdminUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .realName(user.getRealName())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .height(user.getHeight())
                .weight(user.getWeight())
                .activityLevel(user.getActivityLevel())
                .dietGoal(user.getDietGoal())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
