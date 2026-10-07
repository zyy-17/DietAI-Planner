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

    /** 注册，校验用户名和邮箱唯一性后创建账户并签发Token */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }

        // 身份判定：按注册时选择的身份创建账号，缺省为普通用户
        String role = normalizeRole(request.getRole());

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail() != null && !request.getEmail().isEmpty() ? request.getEmail() : null)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
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

        // 登录身份校验：所选身份必须与账号实际角色一致
        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            boolean wantAdmin = "admin".equals(normalizeRole(request.getRole()));
            boolean isAdmin = "admin".equalsIgnoreCase(user.getRole());
            if (wantAdmin && !isAdmin) {
                throw new BusinessException("该账号不是管理员账号，请选择「普通用户」身份登录");
            }
            if (!wantAdmin && isAdmin) {
                throw new BusinessException("该账号是管理员账号，请选择「管理员」身份登录");
            }
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .userId(user.getId())
                .build();
    }

    /** 把前端传来的身份标识归一化为 user / admin，非法值一律按 user 处理 */
    private String normalizeRole(String role) {
        return (role != null && "admin".equalsIgnoreCase(role.trim())) ? "admin" : "user";
    }

    /** 根据ID获取未删除的用户 */
    /**
     * 设定身体目标（目标体重 / 目标体脂 / 期望达成日期）。
     *
     * <p>校验要点：
     * <ul>
     *   <li>目标体重不能与当前体重完全相同，否则没有追踪意义</li>
     *   <li>期望日期不能早于今天</li>
     *   <li>目标与饮食目标方向冲突时给出提示但不强拒——用户可能规划分阶段</li>
     * </ul>
     */
    @Transactional
    public User updateBodyGoal(Long userId, java.math.BigDecimal targetWeightKg,
                               java.math.BigDecimal targetBodyFatPercent,
                               java.time.LocalDate targetDeadline) {
        User user = getUserById(userId);

        if (targetWeightKg != null) {
            if (targetWeightKg.compareTo(new java.math.BigDecimal("20")) < 0
                    || targetWeightKg.compareTo(new java.math.BigDecimal("300")) > 0) {
                throw new BusinessException("目标体重需在 20 ~ 300 kg 之间");
            }
            if (user.getWeight() != null
                    && targetWeightKg.subtract(user.getWeight()).abs()
                        .compareTo(new java.math.BigDecimal("0.1")) < 0) {
                throw new BusinessException("目标体重与当前体重相同，请确认是否填错");
            }
            user.setTargetWeightKg(targetWeightKg);
        }
        if (targetBodyFatPercent != null) {
            if (targetBodyFatPercent.compareTo(java.math.BigDecimal.ZERO) <= 0
                    || targetBodyFatPercent.compareTo(new java.math.BigDecimal("70")) > 0) {
                throw new BusinessException("目标体脂率需在 0 ~ 70 % 之间");
            }
            user.setTargetBodyFatPercent(targetBodyFatPercent);
        }
        if (targetDeadline != null) {
            if (targetDeadline.isBefore(java.time.LocalDate.now())) {
                throw new BusinessException("期望达成日期不能早于今天");
            }
            user.setTargetDeadline(targetDeadline);
        }

        // 目标与饮食目标方向相反时提醒——热量会算反，导致越努力越糟
        if (user.getTargetWeightKg() != null && user.getWeight() != null
                && user.getDietGoal() != null) {
            java.math.BigDecimal diff = user.getTargetWeightKg().subtract(user.getWeight());
            boolean needLower = diff.compareTo(java.math.BigDecimal.ZERO) < 0;
            if (needLower && "gain".equals(user.getDietGoal())) {
                throw new BusinessException("你设置的目标体重比当前轻，但饮食目标是「增肌」。请先到个人中心调整饮食目标");
            }
            if (!needLower && diff.compareTo(java.math.BigDecimal.ZERO) > 0 && "lose".equals(user.getDietGoal())) {
                throw new BusinessException("你设置的目标体重比当前重，但饮食目标是「减脂」。请先到个人中心调整饮食目标");
            }
        }

        return userRepository.save(user);
    }

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
        if (request.getAllergyNote() != null) user.setAllergyNote(request.getAllergyNote());
        if (request.getDisease() != null) user.setDisease(request.getDisease());
        if (request.getMedication() != null) user.setMedication(request.getMedication());
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

    /**
     * 根据饮食目标计算每日目标热量。
     *
     * <p>优先级：用户手动设置值 &gt; 按目标体重与期限推算 &gt; 按固定系数。</p>
     *
     * <p>固定系数（减脂 0.8 / 增肌 1.15）对所有人一样，但"离目标差 5kg"和"差 30kg"
     * 该用的热量完全不同。设了 {@code targetWeightKg} 后改用体重差距推算：</p>
     * <pre>
     *   7700 kcal ≈ 1kg 脂肪；按每天 0.1kg（每周 0.7kg，稳妥速率）折算每日热量差
     *   目标系数 = 1 ± (差距kg × 7700 × 0.1) / TDEE
     * </pre>
     * 结果夹在安全区间内，避免算出过低的热量。
     */
    public java.math.BigDecimal calculateTargetCalories(User user) {
        if (user.getTargetCalories() != null && user.getTargetCalories().compareTo(java.math.BigDecimal.ZERO) > 0) {
            return user.getTargetCalories();
        }
        java.math.BigDecimal tdee = calculateTDEE(user);
        if (tdee.compareTo(java.math.BigDecimal.ZERO) == 0) return java.math.BigDecimal.ZERO;

        String goal = user.getDietGoal() != null ? user.getDietGoal() : "maintain";

        // 优先用目标体重推算
        java.math.BigDecimal byTarget = calcCaloriesByTargetWeight(user, tdee, goal);
        if (byTarget != null) {
            return byTarget.setScale(0, java.math.RoundingMode.HALF_UP);
        }

        java.math.BigDecimal goalFactor;
        switch (goal) {
            case "lose": goalFactor = new java.math.BigDecimal("0.8"); break;
            case "gain": goalFactor = new java.math.BigDecimal("1.15"); break;
            default: goalFactor = java.math.BigDecimal.ONE;
        }
        return tdee.multiply(goalFactor).setScale(0, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 按目标体重与预计达成时间推算目标热量。
     * 缺少必要信息（当前体重 / 目标体重 / 期限）时返回 null，交给固定系数兜底。
     */
    private java.math.BigDecimal calcCaloriesByTargetWeight(User user, java.math.BigDecimal tdee, String goal) {
        java.math.BigDecimal current = user.getWeight();
        java.math.BigDecimal target = user.getTargetWeightKg();
        if (current == null || target == null) return null;
        if (current.compareTo(java.math.BigDecimal.ZERO) <= 0) return null;

        java.math.BigDecimal diffKg = target.subtract(current);
        if (diffKg.abs().compareTo(new java.math.BigDecimal("0.1")) < 0) return null;

        // 计划天数：用户填了期限就用，否则按每周 0.5kg 的温和速率倒推
        long days;
        if (user.getTargetDeadline() != null) {
            days = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(), user.getTargetDeadline());
            if (days < 7) {
                // 期限太近（<1 周）不参与推算，避免算出极端热量
                return null;
            }
        } else {
            java.math.BigDecimal weeks = diffKg.abs()
                    .divide(new java.math.BigDecimal("0.5"), 0, java.math.RoundingMode.CEILING);
            if (weeks.compareTo(java.math.BigDecimal.ZERO) <= 0) return null;
            days = weeks.multiply(java.math.BigDecimal.valueOf(7)).longValueExact();
        }

        // 1kg 脂肪约 7700 kcal
        java.math.BigDecimal totalKcalGap = diffKg.abs().multiply(new java.math.BigDecimal("7700"));
        java.math.BigDecimal dailyGap = totalKcalGap.divide(java.math.BigDecimal.valueOf(days), 2, java.math.RoundingMode.HALF_UP);

        java.math.BigDecimal factor;
        if (diffKg.compareTo(java.math.BigDecimal.ZERO) < 0) {
            // 目标更轻：制造热量缺口
            factor = java.math.BigDecimal.ONE.subtract(
                    dailyGap.divide(tdee, 4, java.math.RoundingMode.HALF_UP));
        } else {
            factor = java.math.BigDecimal.ONE.add(
                    dailyGap.divide(tdee, 4, java.math.RoundingMode.HALF_UP));
        }

        // 安全夹取：减重不低于 TDEE 的 60%，增重不高于 130%
        java.math.BigDecimal lower = new java.math.BigDecimal("0.6");
        java.math.BigDecimal upper = new java.math.BigDecimal("1.3");
        if (factor.compareTo(lower) < 0) factor = lower;
        if (factor.compareTo(upper) > 0) factor = upper;

        // 目标方向与饮食目标矛盾时不用它算（updateBodyGoal 已拦截，这里兜底历史脏数据）
        boolean needLower = diffKg.compareTo(java.math.BigDecimal.ZERO) < 0;
        if (needLower && "gain".equals(goal)) return null;
        if (!needLower && diffKg.compareTo(java.math.BigDecimal.ZERO) > 0 && "lose".equals(goal)) return null;

        return tdee.multiply(factor);
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