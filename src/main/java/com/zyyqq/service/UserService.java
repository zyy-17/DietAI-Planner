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
    private final com.zyyqq.repository.NutritionStandardRepository nutritionStandardRepository;

    /** 用户注册，校验用户名和邮箱唯一性后创建账户并签发Token */
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
                .email(request.getEmail() != null && !request.getEmail().isEmpty() ? request.getEmail() : null)
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

    /** 用户登录，校验账户状态和密码后签发Token */
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

    /** 根据ID获取未删除的用户 */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getDeleted() == 0)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /** 更新用户个人资料，仅更新非空字段 */
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

    /** 计算基础代谢率BMR（Mifflin-St Jeor公式） */
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

    /** 计算每日总消耗TDEE（BMR × 活动系数） */
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

    /** 根据饮食目标计算每日目标热量（优先使用用户手动设置值，否则按TDEE×目标系数计算） */
    public java.math.BigDecimal calculateTargetCalories(User user) {
        if (user.getTargetCalories() != null && user.getTargetCalories().compareTo(java.math.BigDecimal.ZERO) > 0) {
            return user.getTargetCalories();
        }
        com.zyyqq.entity.NutritionStandard standard = findNutritionStandard(user);
        if (standard != null && standard.getCaloriesKcal() != null) {
            String goal = user.getDietGoal() != null ? user.getDietGoal() : "maintain";
            java.math.BigDecimal baseCal = java.math.BigDecimal.valueOf(standard.getCaloriesKcal());
            java.math.BigDecimal goalFactor;
            switch (goal) {
                case "lose": goalFactor = new java.math.BigDecimal("0.8"); break;
                case "gain": goalFactor = new java.math.BigDecimal("1.15"); break;
                default: goalFactor = java.math.BigDecimal.ONE;
            }
            return baseCal.multiply(goalFactor).setScale(0, java.math.RoundingMode.HALF_UP);
        }
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

    /** 查找用户对应的营养标准（按性别+年龄匹配DRIs） */
    public com.zyyqq.entity.NutritionStandard findNutritionStandard(User user) {
        if (user.getGender() == null || user.getBirthDate() == null) return null;
        int age = java.time.Period.between(user.getBirthDate(), java.time.LocalDate.now()).getYears();
        if (age < 0 || age > 120) return null;
        return nutritionStandardRepository.findByGenderAndAge(user.getGender(), age).orElse(null);
    }

    /** 获取目标蛋白质（优先用户设置 > DRIs标准 > 热量推算） */
    public java.math.BigDecimal getTargetProtein(User user) {
        if (user.getTargetProtein() != null && user.getTargetProtein().compareTo(java.math.BigDecimal.ZERO) > 0) {
            return user.getTargetProtein();
        }
        com.zyyqq.entity.NutritionStandard standard = findNutritionStandard(user);
        if (standard != null && standard.getProteinG() != null && standard.getProteinG().compareTo(java.math.BigDecimal.ZERO) > 0) {
            return standard.getProteinG();
        }
        return calculateTargetCalories(user).multiply(new java.math.BigDecimal("0.20")).divide(new java.math.BigDecimal("4"), 1, java.math.RoundingMode.HALF_UP);
    }

    /** 获取目标碳水化合物（优先用户设置 > DRIs标准 > 热量推算） */
    public java.math.BigDecimal getTargetCarbohydrate(User user) {
        if (user.getTargetCarbohydrate() != null && user.getTargetCarbohydrate().compareTo(java.math.BigDecimal.ZERO) > 0) {
            return user.getTargetCarbohydrate();
        }
        com.zyyqq.entity.NutritionStandard standard = findNutritionStandard(user);
        if (standard != null && standard.getCarbG() != null && standard.getCarbG().compareTo(java.math.BigDecimal.ZERO) > 0) {
            return standard.getCarbG();
        }
        return calculateTargetCalories(user).multiply(new java.math.BigDecimal("0.50")).divide(new java.math.BigDecimal("4"), 1, java.math.RoundingMode.HALF_UP);
    }

    /** 获取目标脂肪（优先用户设置 > DRIs标准 > 热量推算） */
    public java.math.BigDecimal getTargetFat(User user) {
        if (user.getTargetFat() != null && user.getTargetFat().compareTo(java.math.BigDecimal.ZERO) > 0) {
            return user.getTargetFat();
        }
        com.zyyqq.entity.NutritionStandard standard = findNutritionStandard(user);
        if (standard != null && standard.getFatG() != null && standard.getFatG().compareTo(java.math.BigDecimal.ZERO) > 0) {
            return standard.getFatG();
        }
        return calculateTargetCalories(user).multiply(new java.math.BigDecimal("0.30")).divide(new java.math.BigDecimal("9"), 1, java.math.RoundingMode.HALF_UP);
    }

    /** 更新用户营养目标（热量/蛋白质/碳水/脂肪） */
    @org.springframework.transaction.annotation.Transactional
    public void updateTarget(Long userId, java.util.Map<String, java.math.BigDecimal> targetMap) {
        User user = getUserById(userId);
        if (targetMap.containsKey("calories")) user.setTargetCalories(targetMap.get("calories"));
        if (targetMap.containsKey("protein")) user.setTargetProtein(targetMap.get("protein"));
        if (targetMap.containsKey("carbohydrate")) user.setTargetCarbohydrate(targetMap.get("carbohydrate"));
        if (targetMap.containsKey("fat")) user.setTargetFat(targetMap.get("fat"));
        userRepository.save(user);
    }

    /** 更新用户偏好设置 */
    @Transactional
    public void updateSettings(Long userId, com.zyyqq.dto.request.UpdateSettingsRequest request) {
        User user = getUserById(userId);
        if (request.getDietReminder() != null) user.setDietReminder(request.getDietReminder());
        if (request.getReminderTime() != null) user.setReminderTime(request.getReminderTime());
        if (request.getGoalReminder() != null) user.setGoalReminder(request.getGoalReminder());
        if (request.getAiSuggestion() != null) user.setAiSuggestion(request.getAiSuggestion());
        if (request.getTheme() != null) user.setTheme(request.getTheme());
        if (request.getLanguage() != null) user.setLanguage(request.getLanguage());
        if (request.getCollapsedSidebar() != null) user.setCollapsedSidebar(request.getCollapsedSidebar());
        if (request.getDataSharing() != null) user.setDataSharing(request.getDataSharing());
        if (request.getPublicRecords() != null) user.setPublicRecords(request.getPublicRecords());
        userRepository.save(user);
    }

    /** 修改密码，校验旧密码正确性 */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("当前密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /** 获取用户偏好设置 */
    public com.zyyqq.dto.response.UserSettingsVO getSettings(Long userId) {
        User user = getUserById(userId);
        return com.zyyqq.dto.response.UserSettingsVO.builder()
                .dietReminder(user.getDietReminder())
                .reminderTime(user.getReminderTime())
                .goalReminder(user.getGoalReminder())
                .aiSuggestion(user.getAiSuggestion())
                .theme(user.getTheme())
                .language(user.getLanguage())
                .collapsedSidebar(user.getCollapsedSidebar())
                .dataSharing(user.getDataSharing())
                .publicRecords(user.getPublicRecords())
                .build();
    }
}