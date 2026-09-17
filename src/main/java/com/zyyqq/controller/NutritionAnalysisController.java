package com.zyyqq.controller;

import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.dto.response.NutritionAnalysisResponse;
import com.zyyqq.service.NutritionAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nutrition")
@RequiredArgsConstructor
public class NutritionAnalysisController {

    private final NutritionAnalysisService nutritionAnalysisService;

    @GetMapping("/analysis")
    public ApiResponse<NutritionAnalysisResponse> analyze(
            Authentication authentication,
            @RequestParam(defaultValue = "7") int days) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(nutritionAnalysisService.analyze(userId, days));
    }

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}