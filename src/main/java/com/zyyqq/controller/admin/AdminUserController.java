package com.zyyqq.controller.admin;

import com.zyyqq.dto.response.AdminUserVO;
import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.entity.User;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端用户管理接口（对应客户端"个人资料 / 设置"模块）。
 * 支持分页查询、关键字搜索、启用禁用、角色调整与密码重置。
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** 分页查询未删除用户，可按用户名/邮箱/真实姓名关键字搜索 */
    @GetMapping
    public ApiResponse<Page<AdminUserVO>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> users;
        if (keyword != null && !keyword.trim().isEmpty()) {
            users = userRepository.searchActiveUsers(keyword.trim(), pageable);
        } else {
            users = userRepository.findByDeletedOrderByCreatedAtDesc(0, pageable);
        }
        return ApiResponse.success(users.map(AdminUserVO::from));
    }

    /** 查询单个用户详情 */
    @GetMapping("/{id}")
    public ApiResponse<AdminUserVO> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return ApiResponse.success(AdminUserVO.from(user));
    }

    /** 启用 / 禁用用户 */
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值不合法");
        }
        user.setStatus(status);
        userRepository.save(user);
        return ApiResponse.success("更新成功", null);
    }

    /** 调整用户角色（user / admin） */
    @PutMapping("/{id}/role")
    public ApiResponse<Void> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        String role = body.get("role");
        if (!"user".equals(role) && !"admin".equals(role)) {
            throw new BusinessException("角色值不合法");
        }
        user.setRole(role);
        userRepository.save(user);
        return ApiResponse.success("更新成功", null);
    }

    /** 重置用户密码 */
    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        String newPassword = body.get("password");
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码至少6位");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ApiResponse.success("密码已重置", null);
    }

    /** 软删除用户（标记为已删除） */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setDeleted(1);
        userRepository.save(user);
        return ApiResponse.success("删除成功", null);
    }
}
