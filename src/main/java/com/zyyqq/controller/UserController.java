package com.zyyqq.controller;

import com.zyyqq.dto.request.ChangePasswordRequest;
import com.zyyqq.dto.request.UpdateProfileRequest;
import com.zyyqq.dto.request.UpdateSettingsRequest;
import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.dto.response.UserProfileVO;
import com.zyyqq.dto.response.UserSettingsVO;
import com.zyyqq.entity.User;
import com.zyyqq.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

    @Value("${app.upload.avatar-url-prefix:/avatars}")
    private String avatarUrlPrefix;

    @GetMapping("/greeting")
    public ApiResponse<java.util.Map<String, String>> getGreetingInfo(Authentication authentication) {
        Long userId = getUserId(authentication);
        User user = userService.getUserById(userId);
        String displayName = (user.getRealName() != null && !user.getRealName().isBlank())
                ? user.getRealName() : user.getUsername();
        return ApiResponse.success(java.util.Map.of("name", displayName));
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileVO> getProfile(Authentication authentication) {
        Long userId = getUserId(authentication);
        User user = userService.getUserById(userId);
        UserProfileVO vo = UserProfileVO.builder()
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
                .dietPreference(user.getDietPreference())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
        return ApiResponse.success(vo);
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfileVO> updateProfile(Authentication authentication,
                                                    @RequestBody UpdateProfileRequest request) {
        Long userId = getUserId(authentication);
        User user = userService.updateProfile(userId, request);
        UserProfileVO vo = UserProfileVO.builder()
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
                .dietPreference(user.getDietPreference())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
        return ApiResponse.success(vo);
    }

    @PostMapping("/avatar")
    public ApiResponse<String> uploadAvatar(Authentication authentication,
                                            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error("请选择文件");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.error("只能上传图片文件");
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            return ApiResponse.error("图片大小不能超过2MB");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        try {
            Path dirPath = Paths.get(avatarDir).toAbsolutePath();
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            Path filePath = dirPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String avatarUrl = avatarUrlPrefix + "/" + filename;

            Long userId = getUserId(authentication);
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setAvatarUrl(avatarUrl);
            userService.updateProfile(userId, request);

            return ApiResponse.success(avatarUrl);
        } catch (IOException e) {
            return ApiResponse.error("头像上传失败: " + e.getMessage());
        }
    }

    /** 获取用户偏好设置 */
    @GetMapping("/settings")
    public ApiResponse<UserSettingsVO> getSettings(Authentication authentication) {
        Long userId = getUserId(authentication);
        UserSettingsVO vo = userService.getSettings(userId);
        return ApiResponse.success(vo);
    }

    @PutMapping("/settings")
    public ApiResponse<Void> updateSettings(Authentication authentication,
                                            @RequestBody UpdateSettingsRequest request) {
        Long userId = getUserId(authentication);
        userService.updateSettings(userId, request);
        return ApiResponse.success("设置已保存", null);
    }

    /** 修改密码，校验新旧密码一致性和长度 */
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(Authentication authentication,
                                            @RequestBody ChangePasswordRequest request) {
        Long userId = getUserId(authentication);
        if (request.getOldPassword() == null || request.getNewPassword() == null) {
            return ApiResponse.error("请填写完整密码信息");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.error("两次密码输入不一致");
        }
        if (request.getNewPassword().length() < 6) {
            return ApiResponse.error("新密码长度不能少于6位");
        }
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success("密码修改成功", null);
    }

    /** 从认证信息中提取用户ID */
    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}