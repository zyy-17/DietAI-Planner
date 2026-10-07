package com.zyyqq.controller;

import com.zyyqq.dto.request.BodyGoalRequest;
import com.zyyqq.dto.request.WeightRecordRequest;
import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.dto.response.WeightTrendVO;
import com.zyyqq.entity.WeightRecord;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.service.UserService;
import com.zyyqq.service.WeightRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 体重/体脂记录与身体目标。
 *
 * <p>挂在 /api/user 下而非独立模块——它记录的是"我的身体数据"，
 * 与个人中心的身高体重同属一类信息。</p>
 */
@RestController
@RequestMapping("/api/user/weight")
@RequiredArgsConstructor
public class WeightRecordController {

    private final WeightRecordService weightRecordService;
    private final UserService userService;

    /** 分页查询历史记录（按日期倒序） */
    @GetMapping("/records")
    public ApiResponse<Page<WeightRecord>> list(Authentication authentication,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(weightRecordService.page(userId, PageRequest.of(page, Math.min(size, 100))));
    }

    /**
     * 保存一条记录（同一天重复提交按覆盖处理）。
     * 同时把体重同步到 user 表，热量目标会跟着更新。
     */
    @PostMapping
    public ApiResponse<WeightRecord> save(Authentication authentication,
                                          @RequestBody WeightRecordRequest request) {
        Long userId = getUserId(authentication);
        LocalDate date = request.getRecordDate() != null ? request.getRecordDate() : LocalDate.now();
        WeightRecord saved = weightRecordService.save(userId, date,
                request.getWeightKg(), request.getBodyFatPercent(), request.getRemark());
        return ApiResponse.success("记录成功", saved);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        weightRecordService.delete(getUserId(authentication), id);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 趋势数据：折线图点 + 区间概览 + 目标进度。
     *
     * @param days 回看天数，默认 90
     */
    @GetMapping("/trend")
    public ApiResponse<WeightTrendVO> trend(Authentication authentication,
                                            @RequestParam(defaultValue = "90") int days) {
        return ApiResponse.success(weightRecordService.trend(getUserId(authentication), days));
    }

    /**
     * 设定身体目标。
     *
     * <p>会校验方向合理性：目标体重填了之后，再改饮食目标若与之矛盾会提示，
     * 但不强制拒绝——用户可能先增重后减脂。</p>
     */
    @PutMapping("/goal")
    public ApiResponse<Map<String, Object>> setGoal(Authentication authentication,
                                                    @RequestBody BodyGoalRequest request) {
        Long userId = getUserId(authentication);
        var user = userService.updateBodyGoal(userId,
                request.getTargetWeightKg(), request.getTargetBodyFatPercent(), request.getTargetDeadline());
        return ApiResponse.success("目标已保存", goalMap(user));
    }

    /** 当前身体目标（页面初始化用） */
    @GetMapping("/goal")
    public ApiResponse<Map<String, Object>> getGoal(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(goalMap(userService.getUserById(userId)));
    }

    private Map<String, Object> goalMap(com.zyyqq.entity.User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("targetWeightKg", user.getTargetWeightKg());
        map.put("targetBodyFatPercent", user.getTargetBodyFatPercent());
        map.put("targetDeadline", user.getTargetDeadline());
        map.put("dietGoal", user.getDietGoal());
        map.put("currentWeight", user.getWeight());
        return map;
    }

    private Long getUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Long)) {
            throw new BusinessException("登录状态异常，请重新登录");
        }
        return (Long) principal;
    }
}
