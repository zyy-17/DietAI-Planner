package com.zyyqq.controller;

import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.dto.response.FoodRecommendationResponse;
import com.zyyqq.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /** 获取个性化食物推荐（Top-N），基于多因素评分算法 */
    @GetMapping("/foods")
    public ApiResponse<FoodRecommendationResponse> recommend(
            Authentication authentication,
            @RequestParam(defaultValue = "5") int topN) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(recommendationService.recommend(userId, topN));
    }

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}