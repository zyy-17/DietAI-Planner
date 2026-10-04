package com.zyyqq.controller;

import com.zyyqq.dto.request.MealPlanAdoptRequest;
import com.zyyqq.dto.request.MealPlanCreateRequest;
import com.zyyqq.dto.request.MealPlanItemReplaceRequest;
import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.dto.response.MealPlanAlternativesVO;
import com.zyyqq.dto.response.MealPlanCurrentVO;
import com.zyyqq.dto.response.MealPlanDayVO;
import com.zyyqq.dto.response.MealPlanDetailVO;
import com.zyyqq.dto.response.MealPlanDraftVO;
import com.zyyqq.dto.response.MealPlanVO;
import com.zyyqq.service.MealPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 膳食方案接口。
 *
 * <p>这个模块只负责三件事：看当前在执行的方案、看今天吃什么、生成/调整方案。
 * 营养分析与 AI 聊天各有自己的入口，不往这里塞。</p>
 */
@RestController
@RequestMapping("/api/meal-plan")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;

    /** 生成方案草稿（不落库，供预览） */
    @PostMapping("/generate")
    public ApiResponse<MealPlanDraftVO> generate(Authentication authentication,
                                                 @RequestBody MealPlanCreateRequest request) {
        return ApiResponse.success(mealPlanService.generateDraft(getUserId(authentication), request));
    }

    /** 采用方案：落库并成为当前执行方案 */
    @PostMapping
    public ApiResponse<MealPlanVO> adopt(Authentication authentication,
                                         @RequestBody MealPlanAdoptRequest request) {
        return ApiResponse.success("方案已采用", mealPlanService.adopt(getUserId(authentication), request));
    }

    /** 当前执行方案 + 今天吃什么 + 今日进度 */
    @GetMapping("/current")
    public ApiResponse<MealPlanCurrentVO> current(Authentication authentication) {
        return ApiResponse.success(mealPlanService.getCurrent(getUserId(authentication)));
    }

    /** 方案列表；status=archived 取历史方案 */
    @GetMapping
    public ApiResponse<List<MealPlanVO>> list(Authentication authentication,
                                              @RequestParam(required = false) String status) {
        return ApiResponse.success(mealPlanService.list(getUserId(authentication), status));
    }

    /** 方案详情：逐天明细 */
    @GetMapping("/{id}")
    public ApiResponse<MealPlanDetailVO> detail(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(mealPlanService.detail(getUserId(authentication), id));
    }

    /** 删除方案 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        mealPlanService.delete(getUserId(authentication), id);
        return ApiResponse.success("已删除", null);
    }

    /** 替换方案里的某一条食物，返回该天的最新明细 */
    @PutMapping("/{planId}/item/{itemId}")
    public ApiResponse<MealPlanDayVO> replaceItem(Authentication authentication,
                                                  @PathVariable Long planId,
                                                  @PathVariable Long itemId,
                                                  @RequestBody MealPlanItemReplaceRequest request) {
        return ApiResponse.success(
                mealPlanService.replaceItem(getUserId(authentication), planId, itemId, request));
    }

    /** AI 智能替换候选 */
    @GetMapping("/{planId}/item/{itemId}/alternatives")
    public ApiResponse<MealPlanAlternativesVO> alternatives(Authentication authentication,
                                                            @PathVariable Long planId,
                                                            @PathVariable Long itemId,
                                                            @RequestParam(required = false) String reason) {
        return ApiResponse.success(
                mealPlanService.alternatives(getUserId(authentication), planId, itemId, reason));
    }

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
