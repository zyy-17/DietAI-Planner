package com.zyyqq.service;

import com.zyyqq.dto.request.LoginRequest;
import com.zyyqq.dto.request.RegisterRequest;
import com.zyyqq.dto.request.UpdateProfileRequest;
import com.zyyqq.dto.response.LoginResponse;
import com.zyyqq.entity.User;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.UserRepository;
import com.zyyqq.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("user")
                .status(1)
                .deleted(0)
                .build();

        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .userId(user.getId())
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameActive(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .userId(user.getId())
                .build();
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getDeleted() == 0)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserById(userId);
        if (request.getRealName() != null) user.setRealName(request.getRealName());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getBirthDate() != null) user.setBirthDate(request.getBirthDate());
        if (request.getHeight() != null) user.setHeight(request.getHeight());
        if (request.getWeight() != null) user.setWeight(request.getWeight());
        if (request.getActivityLevel() != null) user.setActivityLevel(request.getActivityLevel());
        if (request.getDietGoal() != null) user.setDietGoal(request.getDietGoal());
        if (request.getDietPreference() != null) user.setDietPreference(request.getDietPreference());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        return userRepository.save(user);
    }

    public java.math.BigDecimal calculateBMR(User user) {
        if (user.getWeight() == null || user.getHeight() == null || user.getBirthDate() == null || user.getGender() == null) {
            return java.math.BigDecimal.ZERO;
        }
        int age = java.time.Period.between(user.getBirthDate(), java.time.LocalDate.now()).getYears();
        java.math.BigDecimal weight = user.getWeight();
        java.math.BigDecimal height = user.getHeight();

        java.math.BigDecimal bmr;
        if (user.getGender() == 1) {
            bmr = new java.math.BigDecimal("10").multiply(weight)
                    .add(new java.math.BigDecimal("6.25").multiply(height))
                    .subtract(new java.math.BigDecimal("5").multiply(java.math.BigDecimal.valueOf(age)))
                    .add(new java.math.BigDecimal("5"));
        } else {
            bmr = new java.math.BigDecimal("10").multiply(weight)
                    .add(new java.math.BigDecimal("6.25").multiply(height))
                    .subtract(new java.math.BigDecimal("5").multiply(java.math.BigDecimal.valueOf(age)))
                    .subtract(new java.math.BigDecimal("161"));
        }
        return bmr;
    }

    public java.math.BigDecimal calculateTDEE(User user) {
        java.math.BigDecimal bmr = calculateBMR(user);
        if (bmr.compareTo(java.math.BigDecimal.ZERO) == 0) return java.math.BigDecimal.ZERO;

        java.math.BigDecimal activityFactor;
        int level = user.getActivityLevel() != null ? user.getActivityLevel() : 1;
        switch (level) {
            case 1: activityFactor = new java.math.BigDecimal("1.2"); break;
            case 2: activityFactor = new java.math.BigDecimal("1.375"); break;
            case 3: activityFactor = new java.math.BigDecimal("1.55"); break;
            case 4: activityFactor = new java.math.BigDecimal("1.725"); break;
            case 5: activityFactor = new java.math.BigDecimal("1.9"); break;
            default: activityFactor = new java.math.BigDecimal("1.2");
        }
        return bmr.multiply(activityFactor);
    }

    public java.math.BigDecimal calculateTargetCalories(User user) {
        java.math.BigDecimal tdee = calculateTDEE(user);
        if (tdee.compareTo(java.math.BigDecimal.ZERO) == 0) return java.math.BigDecimal.ZERO;

        String goal = user.getDietGoal() != null ? user.getDietGoal() : "maintain";
        java.math.BigDecimal goalFactor;
        switch (goal) {
            case "lose": goalFactor = new java.math.BigDecimal("0.8"); break;
            case "gain": goalFactor = new java.math.BigDecimal("1.15"); break;
            default: goalFactor = java.math.BigDecimal.ONE;
        }
        return tdee.multiply(goalFactor).setScale(0, java.math.RoundingMode.HALF_UP);
    }
}