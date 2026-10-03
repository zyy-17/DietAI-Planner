package com.zyyqq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50位")
    private String username;

    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度6-50位")
    private String password;

    /** 注册身份：user-普通用户 / admin-管理员，不传默认为 user */
    private String role;

    /** 以管理员身份注册时需要提供的邀请码 */
    private String adminInviteCode;
}