package com.zyyqq.service;

import com.zyyqq.dto.response.FoodRecommendationResponse;
import com.zyyqq.entity.DietRecord;
import com.zyyqq.entity.Food;
import com.zyyqq.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final UserService userService;
    private final DietRecordService dietRecordService;
    private final FoodService foodService;
    private final FoodScoreCalculator foodScoreCalculator;

    public FoodRecommendationResponse recommend(Long userId, int topN) {
        long start = System.currentTimeMillis();
        User user = userService.getUserById(userId);

        BigDecimal targetCal = userService.calculateTargetCalories(user);
        BigDecimal targetProtein = resolveTargetProtein(user, targetCal);

        List<DietRecord> todayRecords = dietRecordService.getTodayRecords(userId);
        BigDecimal actualCal = todayRecords.stream().map(DietRecord::getCalories).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actualProtein = todayRecords.stream().map(r -> r.getProtein() != null ? r.getProtein() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingCal = targetCal.subtract(actualCal);
        BigDecimal proteinGap = targetProtein.subtract(actualProtein);

        List<String> avoidFoods = parseAvoidFoods(user.getDietPreference());

        FoodScoreCalculator.RecommendationContext context = FoodScoreCalculator.RecommendationContext.builder()
                .remainingCalories(remainingCal)
                .proteinGap(proteinGap)
                .dietGoal(user.getDietGoal())
                .dietPreference(user.getDietPreference())
                .avoidFoods(avoidFoods)
                .build();

        List<Food> allFoods = foodService.getAllApprovedFoods();
        List<Food> candidateFoods = filterByAvoidList(allFoods, avoidFoods);

        List<FoodScoreCalculator.FoodScoreResult> scoredFoods = foodScoreCalculator.calculateScores(candidateFoods, context);

        List<FoodScoreCalculator.FoodScoreResult> topFoods = scoredFoods.stream()
                .limit(topN > 0 ? topN : 5)
                .collect(Collectors.toList());

        List<FoodRecommendationResponse.ScoredFood> scoredFoodVOs = topFoods.stream()
                .map(sf -> FoodRecommendationResponse.ScoredFood.builder()
                        .foodId(sf.getFoodId())
                        .foodName(sf.getFoodName())
                        .calories(sf.getCalories())
                        .protein(sf.getProtein())
                        .carbohydrate(sf.getCarbohydrate())
                        .fat(sf.getFat())
                        .score(sf.getTotalScore())
                        .build())
                .collect(Collectors.toList());

        List<String> candidateNames = topFoods.stream()
                .map(FoodScoreCalculator.FoodScoreResult::getFoodName)
                .collect(Collectors.toList());

        log.debug("推荐算法执行耗时: {}ms, 候选食物={}, 推荐Top{}", System.currentTimeMillis() - start, candidateFoods.size(), topN);

        return FoodRecommendationResponse.builder()
                .remainingCalories(remainingCal)
                .proteinGap(proteinGap)
                .dietGoal(user.getDietGoal())
                .candidateFoods(candidateNames)
                .recommendations(scoredFoodVOs)
                .build();
    }

    public Map<String, Object> buildAiRecommendContext(Long userId, int topN) {
        FoodRecommendationResponse recommendation = recommend(userId, topN);
        Map<String, Object> context = new HashMap<>();
        context.put("remainingCalories", recommendation.getRemainingCalories());
        context.put("proteinGap", recommendation.getProteinGap());
        context.put("dietGoal", recommendation.getDietGoal());
        context.put("candidateFoods", recommendation.getCandidateFoods());
        return context;
    }

    private BigDecimal resolveTargetProtein(User user, BigDecimal targetCal) {
        if (user.getTargetProtein() != null && user.getTargetProtein().compareTo(BigDecimal.ZERO) > 0) {
            return user.getTargetProtein();
        }
        return targetCal.multiply(new BigDecimal("0.20")).divide(new BigDecimal("4"), 1, RoundingMode.HALF_UP);
    }

    private List<String> parseAvoidFoods(String dietPreference) {
        if (dietPreference == null || dietPreference.isEmpty()) return Collections.emptyList();
        return Arrays.stream(dietPreference.split("[,，、]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<Food> filterByAvoidList(List<Food> foods, List<String> avoidFoods) {
        if (avoidFoods.isEmpty()) return foods;
        return foods.stream()
                .filter(food -> avoidFoods.stream().noneMatch(avoid -> food.getName().contains(avoid)))
                .collect(Collectors.toList());
    }
}