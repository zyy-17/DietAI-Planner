package com.zyyqq.controller;

import com.zyyqq.dto.request.UpdateProfileRequest;
import com.zyyqq.dto.response.ApiResponse;
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

    @GetMapping("/profile")
    public ApiResponse<User> getProfile(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(userService.getUserById(userId));
    }

    @PutMapping("/profile")
    public ApiResponse<User> updateProfile(Authentication authentication,
                                           @RequestBody UpdateProfileRequest request) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(userService.updateProfile(userId, request));
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

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}