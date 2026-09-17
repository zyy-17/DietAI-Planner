package com.zyyqq.controller;

import com.zyyqq.dto.request.UpdateProfileRequest;
import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.entity.User;
import com.zyyqq.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}