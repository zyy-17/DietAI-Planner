package com.zyyqq.service;

import com.zyyqq.dto.request.MealPlanAdoptRequest;
import com.zyyqq.dto.request.MealPlanCreateRequest;
import com.zyyqq.dto.request.MealPlanItemReplaceRequest;
import com.zyyqq.dto.response.FoodAlternativeVO;
import com.zyyqq.dto.response.MealGroupVO;
import com.zyyqq.dto.response.MealPlanAlternativesVO;
import com.zyyqq.dto.response.MealPlanCurrentVO;
import com.zyyqq.dto.response.MealPlanDayVO;
import com.zyyqq.dto.response.MealPlanDetailVO;
import com.zyyqq.dto.response.MealPlanDraftVO;
import com.zyyqq.dto.response.MealPlanItemVO;
import com.zyyqq.dto.response.MealPlanVO;
import com.zyyqq.dto.response.MealProgressVO;
import com.zyyqq.entity.AiGenerationLog;
import com.zyyqq.entity.DietRecord;
import com.zyyqq.entity.Food;
import com.zyyqq.entity.MealPlan;
import com.zyyqq.entity.MealPlanItem;
import com.zyyqq.entity.ProfileOptionType;
import com.zyyqq.entity.User;
import com.zyyqq.entity.UserCustomFood;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.AiGenerationLogRepository;
import com.zyyqq.repository.MealPlanItemRepository;
import com.zyyqq.repository.MealPlanRepository;
import com.zyyqq.repository.UserCustomFoodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 膳食方案：AI 生成 → 预览 → 采用落库 → 逐天执行 → 单项替换。
 *
 * <p>几个刻意的设计：</p>
 * <ul>
 *   <li><b>营养值不由模型给</b>：模型只输出「食物名 + 克数」，热量与三大营养素一律回来
 *       按食物库每 100g 的值换算。模型算数不可信，也不该被写进库里。</li>
 *   <li><b>今日进度不存状态位</b>：直接看今天 {@code diet_record} 里有没有对应餐次，
 *       避免"方案说吃了、记录里没有"的两套账。</li>
 *   <li><b>AI 不可用就明说</b>：生成失败抛明确异常；替换候选退化成算法排序时，
 *       响应里带 {@code aiUsed=false}，由前端如实告知用户，不伪装成 AI 推荐。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MealPlanService {

    /** 允许的方案周期 */
    private static final List<Integer> ALLOWED_DAYS = List.of(3, 7, 14);

    /** 默认餐次顺序，也是展示顺序 */
    private static final List<String> MEAL_ORDER = List.of("breakfast", "lunch", "dinner", "snack");

    private static final Map<String, String> MEAL_LABELS = Map.of(
            "breakfast", "早餐", "lunch", "午餐", "dinner", "晚餐", "snack", "加餐");

    private static final Map<String, String> GOAL_LABELS = Map.of(
            "lose", "减脂", "gain", "增重", "maintain", "维持体重");

    /** 目标对应的热量系数：减脂压低、增重抬高、维持不动 */
    private static final Map<String, BigDecimal> GOAL_FACTORS = Map.of(
            "lose", new BigDecimal("0.80"),
            "gain", new BigDecimal("1.15"),
            "maintain", BigDecimal.ONE);

    /** 最多塞给模型的食物库条目数，防止 prompt 过长拖慢推理 */
    private static final int MAX_CANDIDATE_FOODS = 160;

    /** 单项替换时给模型挑的候选数量 */
    private static final int REPLACE_CANDIDATES = 12;

    /** 按「克/毫升」计量的单位，不参与"1个"这种个数换算 */
    private static final List<String> MEASURE_UNITS = List.of("克", "g", "毫升", "ml", "g/ml");

    private static final Pattern DETAIL_PATTERN = Pattern.compile("\"detail\"\\s*:\\s*\"([^\"]*)\"");

    private final MealPlanRepository planRepository;
    private final MealPlanItemRepository itemRepository;
    private final UserCustomFoodRepository userCustomFoodRepository;
    private final AiGenerationLogRepository generationLogRepository;
    private final UserService userService;
    private final FoodService foodService;
    private final DietRecordService dietRecordService;
    private final ProfileOptionService profileOptionService;
    private final RestTemplate aiRestTemplate;

    @Value("${ai-service.url}")
    private String aiServiceUrl;

    // ══════════════════════════════════════════════════════════════
    //  生成草稿
    // ══════════════════════════════════════════════════════════════

    /**
     * 调 ai-service 生成多天方案草稿，<b>不落库</b>——用户点「采用此方案」才写库。
     * 这里不做事务：中间有一次最长达数分钟的 HTTP 调用，包在事务里会长时间占着数据库连接。
     */
    public MealPlanDraftVO generateDraft(Long userId, MealPlanCreateRequest request) {
        User user = userService.getUserById(userId);

        String goal = normalizeGoal(request.getGoal());
        int days = normalizeDays(request.getDays());
        List<String> meals = normalizeMeals(request.getMeals());
        List<String> preferences = cleanList(request.getPreferences());
        List<String> dislikes = cleanList(request.getDislikes());
        BigDecimal dailyCalories = resolveDailyCalories(user, goal);

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("goal", goal);
        body.put("days", days);
        body.put("meals", meals);
        body.put("daily_calories", dailyCalories.doubleValue());
        body.put("preferences", preferences);
        body.put("dislikes", dislikes);
        body.put("extra_requirement", request.getExtraRequirement());
        body.put("context", buildUserContext(user, goal, dailyCalories));
        body.put("candidate_foods", buildCandidateFoods(userId));

        long start = System.currentTimeMillis();
        Map<?, ?> response = callAiService("/api/meal-plan/generate", body, "生成膳食方案");
        log.info("膳食方案AI生成耗时: {}ms, userId={}, days={}", System.currentTimeMillis() - start, userId, days);

        FoodPool pool = loadFoodPool(userId);
        List<String> unmatched = new ArrayList<>();
        List<MealPlanDayVO> planDays = parseAiDays(response, meals, pool, unmatched);

        String summary = response.get("summary") != null ? String.valueOf(response.get("summary")).trim() : "";
        if (summary.isBlank()) {
            summary = buildFallbackSummary(planDays, dailyCalories);
        }

        MealPlanDraftVO draft = MealPlanDraftVO.builder()
                .name(days + "天" + GOAL_LABELS.get(goal) + "计划")
                .summary(summary)
                .goal(goal)
                .goalLabel(GOAL_LABELS.get(goal))
                .days(days)
                .meals(meals)
                .dailyCalories(dailyCalories)
                .preferences(preferences)
                .dislikes(dislikes)
                .extraRequirement(request.getExtraRequirement())
                .planDays(planDays)
                .unmatchedFoods(unmatched)
                .build();

        writeGenerationLog(userId, String.format("生成%d天%s方案", days, GOAL_LABELS.get(goal)),
                String.format("共%d天/%d条食物，概述：%s", planDays.size(), countItems(planDays), summary));
        return draft;
    }

    // ══════════════════════════════════════════════════════════════
    //  采用方案
    // ══════════════════════════════════════════════════════════════

    /** 采用方案：写入库并把旧的执行中方案归档进历史。 */
    @Transactional
    public MealPlanVO adopt(Long userId, MealPlanAdoptRequest request) {
        if (request.getDays() == null || request.getDays().isEmpty()) {
            throw new BusinessException("方案内容为空，无法采用");
        }
        String goal = normalizeGoal(request.getGoal());
        List<String> meals = normalizeMeals(request.getMeals());

        // 同一用户同时只保留一份执行中方案，旧的转历史
        List<MealPlan> actives = planRepository.findByUserIdAndStatus(userId, MealPlan.STATUS_ACTIVE);
        for (MealPlan old : actives) {
            old.setStatus(MealPlan.STATUS_ARCHIVED);
        }
        if (!actives.isEmpty()) {
            planRepository.saveAll(actives);
        }

        int days = request.getDays().size();
        MealPlan plan = MealPlan.builder()
                .userId(userId)
                .name(request.getName() != null && !request.getName().isBlank()
                        ? request.getName() : days + "天" + GOAL_LABELS.get(goal) + "计划")
                .goal(goal)
                .days(days)
                .meals(String.join(",", meals))
                .dailyCalories(request.getDailyCalories())
                .preferences(String.join("、", cleanList(request.getPreferences())))
                .dislikes(String.join("、", cleanList(request.getDislikes())))
                .extraRequirement(request.getExtraRequirement())
                .summary(request.getSummary())
                .status(MealPlan.STATUS_ACTIVE)
                .startDate(LocalDate.now())
                .build();
        plan = planRepository.save(plan);

        FoodPool pool = loadFoodPool(userId);
        List<MealPlanItem> items = new ArrayList<>();
        int unmatchedCount = 0;
        for (MealPlanAdoptRequest.Day day : request.getDays()) {
            if (day == null || day.getDay() == null || day.getMeals() == null) {
                continue;
            }
            for (String mealType : meals) {
                List<MealPlanAdoptRequest.Item> dayItems = day.getMeals().get(mealType);
                if (dayItems == null) {
                    continue;
                }
                int order = 0;
                for (MealPlanAdoptRequest.Item raw : dayItems) {
                    FoodRef ref = pool.match(raw.getFood());
                    if (ref == null || raw.getAmount() == null || raw.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        unmatchedCount++;
                        continue;
                    }
                    items.add(buildItem(plan.getId(), day.getDay(), mealType, ref, raw.getAmount(), order++));
                }
            }
        }
        itemRepository.saveAll(items);
        log.info("采用膳食方案: userId={}, planId={}, 条目={}, 未匹配={}", userId, plan.getId(), items.size(), unmatchedCount);

        MealPlanVO vo = toPlanVO(plan);
        vo.setItemCount(items.size());
        vo.setPlannedDailyCalories(averageDailyCalories(items, days));
        return vo;
    }

    // ══════════════════════════════════════════════════════════════
    //  查询
    // ══════════════════════════════════════════════════════════════

    /** 当前执行方案 + 今天吃什么 + 今日进度；没有方案时 plan 为 null。 */
    public MealPlanCurrentVO getCurrent(Long userId) {
        Optional<MealPlan> active = planRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, MealPlan.STATUS_ACTIVE);
        if (active.isEmpty()) {
            return MealPlanCurrentVO.builder().todayDate(LocalDate.now()).build();
        }
        MealPlan plan = active.get();
        List<MealPlanItem> allItems = itemRepository.findByPlanIdOrderByDayIndexAscSortOrderAscIdAsc(plan.getId());
        List<String> meals = splitMeals(plan.getMeals());

        int currentDay = resolveCurrentDayIndex(plan);
        List<MealPlanItem> todayItems = allItems.stream()
                .filter(i -> i.getDayIndex() != null && i.getDayIndex() == currentDay)
                .toList();

        List<DietRecord> todayRecords = dietRecordService.getTodayRecords(userId);
        Map<String, Long> todayByMeal = new HashMap<>();
        for (DietRecord record : todayRecords) {
            todayByMeal.merge(record.getMealType(), 1L, Long::sum);
        }
        List<MealProgressVO> progress = new ArrayList<>();
        for (String mealType : meals) {
            long count = todayByMeal.getOrDefault(mealType, 0L);
            progress.add(MealProgressVO.builder()
                    .mealType(mealType)
                    .mealLabel(mealLabel(mealType))
                    .done(count > 0)
                    .recordCount((int) count)
                    .build());
        }

        BigDecimal intake = todayRecords.stream()
                .map(r -> r.getCalories() != null ? r.getCalories() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MealPlanVO planVO = toPlanVO(plan);
        planVO.setItemCount(allItems.size());
        planVO.setPlannedDailyCalories(averageDailyCalories(allItems, plan.getDays()));

        return MealPlanCurrentVO.builder()
                .plan(planVO)
                .today(buildDayVO(currentDay, todayItems, meals))
                .currentDayIndex(currentDay)
                .todayDate(LocalDate.now())
                .progress(progress)
                .todayIntakeCalories(intake)
                .build();
    }

    /** 方案列表；status 为空时返回全部（历史方案页签传 archived）。 */
    public List<MealPlanVO> list(Long userId, String status) {
        List<MealPlan> plans = (status == null || status.isBlank())
                ? planRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : planRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        if (plans.isEmpty()) {
            return List.of();
        }
        List<Long> ids = plans.stream().map(MealPlan::getId).toList();
        Map<Long, List<MealPlanItem>> itemsByPlan = new HashMap<>();
        for (MealPlanItem item : itemRepository.findByPlanIdInOrderByDayIndexAscSortOrderAscIdAsc(ids)) {
            itemsByPlan.computeIfAbsent(item.getPlanId(), k -> new ArrayList<>()).add(item);
        }
        List<MealPlanVO> result = new ArrayList<>();
        for (MealPlan plan : plans) {
            List<MealPlanItem> items = itemsByPlan.getOrDefault(plan.getId(), List.of());
            MealPlanVO vo = toPlanVO(plan);
            vo.setItemCount(items.size());
            vo.setPlannedDailyCalories(averageDailyCalories(items, plan.getDays()));
            result.add(vo);
        }
        return result;
    }

    /** 方案详情：逐天明细。 */
    public MealPlanDetailVO detail(Long userId, Long planId) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        List<String> meals = splitMeals(plan.getMeals());
        List<MealPlanItem> all = itemRepository.findByPlanIdOrderByDayIndexAscSortOrderAscIdAsc(planId);

        Map<Integer, List<MealPlanItem>> byDay = new LinkedHashMap<>();
        for (MealPlanItem item : all) {
            byDay.computeIfAbsent(item.getDayIndex() == null ? 1 : item.getDayIndex(), k -> new ArrayList<>()).add(item);
        }
        List<MealPlanDayVO> planDays = new ArrayList<>();
        for (int day = 1; day <= plan.getDays(); day++) {
            planDays.add(buildDayVO(day, byDay.getOrDefault(day, List.of()), meals));
        }

        MealPlanVO vo = toPlanVO(plan);
        vo.setItemCount(all.size());
        vo.setPlannedDailyCalories(averageDailyCalories(all, plan.getDays()));
        return MealPlanDetailVO.builder().plan(vo).planDays(planDays).build();
    }

    /** 删除方案（历史方案清理用）。 */
    @Transactional
    public void delete(Long userId, Long planId) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        itemRepository.deleteByPlanId(plan.getId());
        planRepository.delete(plan);
        log.info("删除膳食方案: userId={}, planId={}", userId, planId);
    }

    // ══════════════════════════════════════════════════════════════
    //  单项替换
    // ══════════════════════════════════════════════════════════════

    /** 用指定食物替换某一条，按食物库重新换算营养值，返回该天的最新明细。 */
    @Transactional
    public MealPlanDayVO replaceItem(Long userId, Long planId, Long itemId, MealPlanItemReplaceRequest request) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        MealPlanItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("该食物条目不存在"));
        if (!item.getPlanId().equals(plan.getId())) {
            throw new BusinessException("该食物条目不属于此方案");
        }

        FoodPool pool = loadFoodPool(userId);
        FoodRef ref = resolveReplaceTarget(pool, request);
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : item.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("克数必须大于 0");
        }

        applyFoodRef(item, ref, amount);
        itemRepository.save(item);
        log.info("替换方案食物: planId={}, itemId={}, {} -> {}", planId, itemId, item.getFoodName(), ref.name());

        List<MealPlanItem> dayItems = itemRepository
                .findByPlanIdAndDayIndexOrderBySortOrderAscIdAsc(planId, item.getDayIndex());
        return buildDayVO(item.getDayIndex(), dayItems, splitMeals(plan.getMeals()));
    }

    /**
     * 「AI 智能替换」候选：先从食物库按「同类目 + 热量接近」筛一批，
     * 再让模型排序并给理由。模型不可用时退化为算法排序，并在响应里标明 aiUsed=false。
     */
    public MealPlanAlternativesVO alternatives(Long userId, Long planId, Long itemId, String reason) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        MealPlanItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("该食物条目不存在"));
        if (!item.getPlanId().equals(plan.getId())) {
            throw new BusinessException("该食物条目不属于此方案");
        }

        User user = userService.getUserById(userId);
        FoodPool pool = loadFoodPool(userId);
        List<String> dislikes = collectDislikes(user, plan);
        List<FoodRef> candidates = pickReplaceCandidates(pool, item, dislikes);
        if (candidates.isEmpty()) {
            throw new BusinessException("食物库里没有可替换的同类食物");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("food_name", item.getFoodName());
        body.put("amount", item.getAmount() != null ? item.getAmount().doubleValue() : 0);
        body.put("reason", reason);
        body.put("goal", plan.getGoal());
        body.put("meal_type", item.getMealType());
        body.put("meal_desc", describeMeal(plan, item));
        body.put("preferences", splitTags(plan.getPreferences()));
        body.put("dislikes", dislikes);
        body.put("candidates", candidates.stream().map(MealPlanService::formatFoodLine).toList());

        try {
            Map<?, ?> response = callAiService("/api/meal-plan/replace", body, "AI 智能替换");
            List<FoodAlternativeVO> alternatives = new ArrayList<>();
            Object raw = response.get("recommendations");
            if (raw instanceof List<?> list) {
                for (Object entry : list) {
                    if (!(entry instanceof Map<?, ?> row)) {
                        continue;
                    }
                    FoodRef ref = pool.match(str(row.get("food")));
                    if (ref == null || ref.name().equals(item.getFoodName())) {
                        continue;
                    }
                    BigDecimal amount = toDecimal(row.get("amount"));
                    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        amount = item.getAmount();
                    }
                    alternatives.add(buildAlternative(ref, amount, str(row.get("reason"))));
                }
            }
            if (alternatives.isEmpty()) {
                throw new IllegalStateException("模型没有给出可用的替代食物");
            }
            return MealPlanAlternativesVO.builder()
                    .originalFoodName(item.getFoodName())
                    .aiUsed(true)
                    .candidates(alternatives)
                    .build();
        } catch (Exception e) {
            log.warn("AI 替换推荐失败，退化为按热量接近度排序: {}", e.getMessage());
            List<FoodAlternativeVO> fallback = candidates.stream()
                    .limit(4)
                    .map(ref -> buildAlternative(ref, item.getAmount(), ""))
                    .toList();
            return MealPlanAlternativesVO.builder()
                    .originalFoodName(item.getFoodName())
                    .aiUsed(false)
                    .note("AI 这次没有响应，以下是系统按「同类目 + 热量接近」挑出的候选")
                    .candidates(fallback)
                    .build();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  AI 调用
    // ══════════════════════════════════════════════════════════════

    private Map<?, ?> callAiService(String path, Map<String, Object> body, String tag) {
        try {
            Map<?, ?> response = aiRestTemplate.postForObject(aiServiceUrl + path, body, Map.class);
            if (response == null) {
                throw new BusinessException(tag + "失败：AI 服务返回空内容，请稍后重试");
            }
            return response;
        } catch (HttpStatusCodeException e) {
            // ai-service 把出错原因放在 {"detail": "..."} 里，拿出来给用户看，别吞成一句"系统错误"
            String detail = str(e.getResponseBodyAsString());
            Matcher matcher = DETAIL_PATTERN.matcher(detail);
            if (matcher.find()) {
                detail = matcher.group(1);
            }
            log.warn("{}失败: status={}, detail={}", tag, e.getStatusCode(), detail);
            throw new BusinessException(tag + "失败：" + detail);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("{}失败: {}", tag, e.getMessage());
            throw new BusinessException(tag + "失败：AI 服务未响应（" + e.getMessage() + "），请确认 ai-service 已启动后重试");
        }
    }

    private void writeGenerationLog(Long userId, String inputSummary, String output) {
        try {
            generationLogRepository.save(AiGenerationLog.builder()
                    .userId(userId)
                    .type("diet_plan")
                    .inputSummary(inputSummary)
                    .outputContent(output)
                    .modelName("ai-service")
                    .isAbnormal(0)
                    .build());
        } catch (Exception e) {
            log.warn("写 AI 生成日志失败: {}", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  AI 输出解析
    // ══════════════════════════════════════════════════════════════

    private List<MealPlanDayVO> parseAiDays(Map<?, ?> response, List<String> meals,
                                            FoodPool pool, List<String> unmatched) {
        Object rawDays = response.get("days");
        if (!(rawDays instanceof List<?> list) || list.isEmpty()) {
            throw new BusinessException("AI 返回的方案为空，请重试");
        }
        List<MealPlanDayVO> planDays = new ArrayList<>();
        int order = 0;
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> dayRow)) {
                continue;
            }
            Integer dayIndex = toInt(dayRow.get("day"));
            if (dayIndex == null) {
                continue;
            }
            Object rawMeals = dayRow.get("meals");
            if (!(rawMeals instanceof Map<?, ?> mealMap)) {
                continue;
            }
            List<MealGroupVO> groups = new ArrayList<>();
            for (String mealType : meals) {
                Object rawItems = mealMap.get(mealType);
                if (!(rawItems instanceof List<?> itemList)) {
                    continue;
                }
                List<MealPlanItemVO> itemVOs = new ArrayList<>();
                for (Object rawItem : itemList) {
                    if (!(rawItem instanceof Map<?, ?> itemRow)) {
                        continue;
                    }
                    String name = str(itemRow.get("food"));
                    BigDecimal amount = toDecimal(itemRow.get("amount"));
                    if (name.isBlank() || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    FoodRef ref = pool.match(name);
                    if (ref == null) {
                        if (!unmatched.contains(name)) {
                            unmatched.add(name);
                        }
                        continue;
                    }
                    itemVOs.add(draftItemVO(ref, amount));
                }
                if (!itemVOs.isEmpty()) {
                    groups.add(MealGroupVO.builder()
                            .mealType(mealType)
                            .mealLabel(mealLabel(mealType))
                            .calories(sum(itemVOs, MealPlanItemVO::getCalories))
                            .items(itemVOs)
                            .build());
                }
            }
            if (!groups.isEmpty()) {
                planDays.add(dayVO(dayIndex, groups));
            }
        }
        if (planDays.isEmpty()) {
            throw new BusinessException("AI 返回的食物都不在食物库里，未能生成方案，请重试");
        }
        return planDays;
    }

    private String buildFallbackSummary(List<MealPlanDayVO> planDays, BigDecimal target) {
        if (planDays.isEmpty()) {
            return "";
        }
        BigDecimal sum = planDays.stream()
                .map(d -> d.getCalories() != null ? d.getCalories() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(planDays.size()), 0, RoundingMode.HALF_UP);
        return "每日约 " + avg + " kcal（目标 " + target.setScale(0, RoundingMode.HALF_UP) + " kcal），按餐次搭配主食、蛋白质与蔬菜";
    }

    // ══════════════════════════════════════════════════════════════
    //  食物库
    // ══════════════════════════════════════════════════════════════

    /** 食物库里的一项，屏蔽 Food 与 UserCustomFood 两张表的差异。 */
    private record FoodRef(Long id, String source, String name, Long categoryId,
                           BigDecimal calories, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat,
                           String unitName, BigDecimal unitWeight) {
    }

    /**
     * 已加载的食物池。
     * 除精确名索引外，也保留全量列表用于模糊匹配（模型偶尔会写「燕麦片」而库里是「燕麦」）。
     */
    private record FoodPool(List<FoodRef> all, Map<String, FoodRef> exact, Map<String, FoodRef> byKey) {

        FoodRef match(String name) {
            if (name == null || name.isBlank()) {
                return null;
            }
            String key = name.trim();
            FoodRef hit = exact.get(key);
            if (hit != null) {
                return hit;
            }
            for (FoodRef ref : all) {
                if (ref.name().contains(key) || key.contains(ref.name())) {
                    return ref;
                }
            }
            return null;
        }

        /** 按「来源:ID」取，替换时前端回传 foodId 用 */
        FoodRef byId(String source, Long id) {
            return id == null ? null : byKey.get(source + ":" + id);
        }
    }

    private FoodPool loadFoodPool(Long userId) {
        List<FoodRef> all = new ArrayList<>();
        Map<String, FoodRef> exact = new LinkedHashMap<>();
        Map<String, FoodRef> byKey = new HashMap<>();
        for (Food food : foodService.getAllApprovedFoods()) {
            FoodRef ref = new FoodRef(food.getId(), MealPlanItem.SOURCE_SYSTEM, food.getName(), food.getCategoryId(),
                    food.getCalories(), food.getProtein(), food.getCarbohydrate(), food.getFat(),
                    food.getUnitName(), food.getUnitWeight());
            all.add(ref);
            exact.putIfAbsent(ref.name(), ref);
            byKey.put(ref.source() + ":" + ref.id(), ref);
        }
        for (UserCustomFood custom : userCustomFoodRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            FoodRef ref = new FoodRef(custom.getId(), MealPlanItem.SOURCE_USER, custom.getName(), custom.getCategoryId(),
                    custom.getCalories(), custom.getProtein(), custom.getCarbohydrate(), custom.getFat(),
                    custom.getUnitName(), custom.getUnitWeight());
            all.add(ref);
            exact.putIfAbsent(ref.name(), ref);
            byKey.put(ref.source() + ":" + ref.id(), ref);
        }
        return new FoodPool(all, exact, byKey);
    }

    /**
     * 给模型的食物库清单。
     * 按分类轮流取而不是顺着表取——否则库里主食最多时，模型看到的可能全是主食，
     * 搭配不出「主食 + 蛋白质 + 蔬菜」。
     */
    private List<String> buildCandidateFoods(Long userId) {
        Map<Long, List<Food>> byCategory = new LinkedHashMap<>();
        for (Food food : foodService.getAllApprovedFoods()) {
            byCategory.computeIfAbsent(food.getCategoryId(), k -> new ArrayList<>()).add(food);
        }
        List<Food> picked = new ArrayList<>();
        int round = 0;
        boolean progressed = true;
        while (picked.size() < MAX_CANDIDATE_FOODS && progressed) {
            progressed = false;
            for (List<Food> group : byCategory.values()) {
                if (picked.size() >= MAX_CANDIDATE_FOODS) {
                    break;
                }
                if (round < group.size()) {
                    picked.add(group.get(round));
                    progressed = true;
                }
            }
            round++;
        }

        List<String> lines = new ArrayList<>();
        for (Food food : picked) {
            lines.add(formatFoodLine(food.getName(), food.getCalories(), food.getProtein(),
                    food.getCarbohydrate(), food.getFat()));
        }
        // 用户自己的食物也带上：他常吃的东西更可能被接受
        for (UserCustomFood custom : userCustomFoodRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            lines.add(formatFoodLine(custom.getName(), custom.getCalories(), custom.getProtein(),
                    custom.getCarbohydrate(), custom.getFat()));
        }
        return lines;
    }

    /** 拼成「名称|每100g热量|蛋白/碳水/脂肪」一行，给模型一个可核对的数据源。 */
    private static String formatFoodLine(FoodRef ref) {
        return formatFoodLine(ref.name(), ref.calories(), ref.protein(), ref.carbohydrate(), ref.fat());
    }

    private static String formatFoodLine(String name, BigDecimal calories, BigDecimal protein,
                                         BigDecimal carbohydrate, BigDecimal fat) {
        return String.format("%s|%s|%s/%s/%s", name,
                plain(calories), plain(protein), plain(carbohydrate), plain(fat));
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    /**
     * 挑替换候选：优先同分类，不足时放宽到全部；排除原食物与忌口，再按热量接近度排序。
     */
    private List<FoodRef> pickReplaceCandidates(FoodPool pool, MealPlanItem item, List<String> dislikes) {
        FoodRef origin = pool.match(item.getFoodName());
        List<FoodRef> sameCategory = new ArrayList<>();
        List<FoodRef> others = new ArrayList<>();
        for (FoodRef ref : pool.all()) {
            if (ref.name().equals(item.getFoodName()) || isDisliked(ref.name(), dislikes)) {
                continue;
            }
            if (origin != null && origin.categoryId() != null && origin.categoryId().equals(ref.categoryId())) {
                sameCategory.add(ref);
            } else {
                others.add(ref);
            }
        }
        BigDecimal originKcal = origin != null && origin.calories() != null ? origin.calories() : item.getCalories();
        // 排序键：与「原食物每100g热量」的差值，越小说明替换后这一餐的热量变化越小
        double originValue = kcalOf(originKcal).doubleValue();
        Comparator<FoodRef> byKcalDistance =
                Comparator.comparingDouble(ref -> Math.abs(kcalOf(ref.calories()).doubleValue() - originValue));
        sameCategory.sort(byKcalDistance);
        others.sort(byKcalDistance);

        List<FoodRef> picked = new ArrayList<>();
        for (FoodRef ref : sameCategory) {
            if (picked.size() >= REPLACE_CANDIDATES) {
                break;
            }
            picked.add(ref);
        }
        for (FoodRef ref : others) {
            if (picked.size() >= REPLACE_CANDIDATES) {
                break;
            }
            picked.add(ref);
        }
        return picked;
    }

    private static BigDecimal kcalOf(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private boolean isDisliked(String foodName, List<String> dislikes) {
        if (foodName == null) {
            return false;
        }
        for (String dislike : dislikes) {
            if (!dislike.isBlank() && (foodName.contains(dislike) || dislike.contains(foodName))) {
                return true;
            }
        }
        return false;
    }

    private FoodRef resolveReplaceTarget(FoodPool pool, MealPlanItemReplaceRequest request) {
        FoodRef ref = pool.byId(request.getFoodSource(), request.getFoodId());
        if (ref == null) {
            ref = pool.match(request.getFoodName());
        }
        if (ref == null) {
            throw new BusinessException("食物库里找不到「" + request.getFoodName() + "」，请从食物库里选择");
        }
        return ref;
    }

    // ══════════════════════════════════════════════════════════════
    //  组装 VO
    // ══════════════════════════════════════════════════════════════

    private MealPlanItem buildItem(Long planId, Integer dayIndex, String mealType,
                                   FoodRef ref, BigDecimal amount, int sortOrder) {
        MealPlanItem item = MealPlanItem.builder()
                .planId(planId)
                .dayIndex(dayIndex)
                .mealType(mealType)
                .foodId(ref.id())
                .foodSource(ref.source())
                .foodName(ref.name())
                .unitName(ref.unitName())
                .unitWeight(ref.unitWeight())
                .amount(amount)
                .sortOrder(sortOrder)
                .build();
        applyNutrition(item, ref, amount);
        return item;
    }

    /** 把条目切换成另一种食物，并按新食物重算营养值。 */
    private void applyFoodRef(MealPlanItem item, FoodRef ref, BigDecimal amount) {
        item.setFoodId(ref.id());
        item.setFoodSource(ref.source());
        item.setFoodName(ref.name());
        item.setUnitName(ref.unitName());
        item.setUnitWeight(ref.unitWeight());
        item.setAmount(amount);
        applyNutrition(item, ref, amount);
    }

    private void applyNutrition(MealPlanItem item, FoodRef ref, BigDecimal amount) {
        item.setCalories(scale(ref.calories(), amount));
        item.setProtein(scale(ref.protein(), amount));
        item.setCarbohydrate(scale(ref.carbohydrate(), amount));
        item.setFat(scale(ref.fat(), amount));
    }

    private MealPlanItemVO draftItemVO(FoodRef ref, BigDecimal amount) {
        return MealPlanItemVO.builder()
                .foodId(ref.id())
                .foodSource(ref.source())
                .foodName(ref.name())
                .amount(amount)
                .amountText(amountText(amount, ref.unitName(), ref.unitWeight()))
                .calories(scale(ref.calories(), amount))
                .protein(scale(ref.protein(), amount))
                .carbohydrate(scale(ref.carbohydrate(), amount))
                .fat(scale(ref.fat(), amount))
                .build();
    }

    private MealPlanItemVO toItemVO(MealPlanItem item) {
        return MealPlanItemVO.builder()
                .id(item.getId())
                .foodId(item.getFoodId())
                .foodSource(item.getFoodSource())
                .foodName(item.getFoodName())
                .amount(item.getAmount())
                .amountText(amountText(item.getAmount(), item.getUnitName(), item.getUnitWeight()))
                .calories(item.getCalories())
                .protein(item.getProtein())
                .carbohydrate(item.getCarbohydrate())
                .fat(item.getFat())
                .build();
    }

    private MealPlanDayVO buildDayVO(Integer dayIndex, List<MealPlanItem> items, List<String> meals) {
        List<MealGroupVO> groups = new ArrayList<>();
        for (String mealType : meals) {
            List<MealPlanItemVO> mealItems = items.stream()
                    .filter(i -> mealType.equals(i.getMealType()))
                    .sorted(Comparator.comparingInt(i -> i.getSortOrder() == null ? 0 : i.getSortOrder()))
                    .map(this::toItemVO)
                    .toList();
            if (mealItems.isEmpty()) {
                continue;
            }
            groups.add(MealGroupVO.builder()
                    .mealType(mealType)
                    .mealLabel(mealLabel(mealType))
                    .calories(sum(mealItems, MealPlanItemVO::getCalories))
                    .items(mealItems)
                    .build());
        }
        return dayVO(dayIndex, groups);
    }

    private MealPlanDayVO dayVO(Integer dayIndex, List<MealGroupVO> groups) {
        List<MealPlanItemVO> items = groups.stream().flatMap(g -> g.getItems().stream()).toList();
        return MealPlanDayVO.builder()
                .dayIndex(dayIndex)
                .calories(sum(items, MealPlanItemVO::getCalories))
                .protein(sum(items, MealPlanItemVO::getProtein))
                .carbohydrate(sum(items, MealPlanItemVO::getCarbohydrate))
                .fat(sum(items, MealPlanItemVO::getFat))
                .meals(groups)
                .build();
    }

    private MealPlanVO toPlanVO(MealPlan plan) {
        return MealPlanVO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .summary(plan.getSummary())
                .goal(plan.getGoal())
                .goalLabel(GOAL_LABELS.getOrDefault(plan.getGoal(), plan.getGoal()))
                .days(plan.getDays())
                .meals(splitMeals(plan.getMeals()))
                .dailyCalories(plan.getDailyCalories())
                .startDate(plan.getStartDate())
                .status(plan.getStatus())
                .createdAt(plan.getCreatedAt())
                .currentDayIndex(resolveCurrentDayIndex(plan))
                .build();
    }

    private FoodAlternativeVO buildAlternative(FoodRef ref, BigDecimal amount, String reason) {
        return FoodAlternativeVO.builder()
                .foodId(ref.id())
                .foodSource(ref.source())
                .foodName(ref.name())
                .amount(amount)
                .amountText(amountText(amount, ref.unitName(), ref.unitWeight()))
                .calories(scale(ref.calories(), amount))
                .protein(scale(ref.protein(), amount))
                .carbohydrate(scale(ref.carbohydrate(), amount))
                .fat(scale(ref.fat(), amount))
                .reason(reason == null ? "" : reason)
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    //  小工具
    // ══════════════════════════════════════════════════════════════

    private MealPlan requireOwnedPlan(Long userId, Long planId) {
        MealPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("方案不存在"));
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException("无权访问此方案");
        }
        return plan;
    }

    /** 今天是方案第几天；超出周期后停在最后一天，避免详情页定位到空白 */
    private int resolveCurrentDayIndex(MealPlan plan) {
        if (plan.getStartDate() == null) {
            return 1;
        }
        long offset = ChronoUnit.DAYS.between(plan.getStartDate(), LocalDate.now()) + 1;
        int days = plan.getDays() == null || plan.getDays() < 1 ? 1 : plan.getDays();
        return (int) Math.min(Math.max(offset, 1), days);
    }

    /** 每日目标热量：目标与档案一致时直接用档案算出来的值，不一致就按新目标系数折算 */
    private BigDecimal resolveDailyCalories(User user, String goal) {
        BigDecimal base = userService.calculateTargetCalories(user);
        if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            base = new BigDecimal("1800");
        }
        String profileGoal = user.getDietGoal() == null ? "maintain" : user.getDietGoal();
        if (goal.equals(profileGoal)) {
            return base.setScale(0, RoundingMode.HALF_UP);
        }
        BigDecimal tdee = userService.calculateTDEE(user);
        BigDecimal source = (tdee != null && tdee.compareTo(BigDecimal.ZERO) > 0) ? tdee : base;
        return source.multiply(GOAL_FACTORS.getOrDefault(goal, BigDecimal.ONE)).setScale(0, RoundingMode.HALF_UP);
    }

    /** 用户档案摘要：只放跟"配餐"有关的信息，不塞 7 天饮食明细，避免 prompt 臃肿 */
    private String buildUserContext(User user, String goal, BigDecimal dailyCalories) {
        StringBuilder sb = new StringBuilder("用户画像：");
        if (user.getGender() != null) {
            sb.append("性别=").append(user.getGender() == 1 ? "男" : "女").append(", ");
        }
        if (user.getBirthDate() != null) {
            int age = java.time.Period.between(user.getBirthDate(), LocalDate.now()).getYears();
            sb.append("年龄=").append(age).append("岁, ");
        }
        if (user.getHeight() != null) {
            sb.append("身高=").append(plain(user.getHeight())).append("cm, ");
        }
        if (user.getWeight() != null) {
            sb.append("体重=").append(plain(user.getWeight())).append("kg, ");
        }
        String activity = profileOptionService.labelOf(ProfileOptionType.ACTIVITY_LEVEL,
                user.getActivityLevel() != null ? String.valueOf(user.getActivityLevel()) : null);
        if (activity != null && !activity.isBlank()) {
            sb.append("活动水平=").append(activity).append(", ");
        }
        sb.append("本次配餐目标=").append(GOAL_LABELS.get(goal));
        sb.append("，每日目标热量=").append(dailyCalories).append("kcal");

        String allergy = profileOptionService.labelsOf(ProfileOptionType.ALLERGY, user.getAllergyNote());
        if (allergy != null && !allergy.isBlank()) {
            sb.append("\n档案里的忌口（绝对不能吃）：").append(allergy);
        }
        String disease = profileOptionService.labelsOf(ProfileOptionType.DISEASE, user.getDisease());
        if (disease != null && !disease.isBlank()) {
            sb.append("\n慢性疾病（配餐要注意）：").append(disease);
        }
        if (user.getMedication() != null && !user.getMedication().isBlank()) {
            sb.append("\n用药情况：").append(user.getMedication());
        }
        return sb.toString();
    }

    /** 忌口来源有两处：档案里的忌口 + 创建方案时勾的"不喜欢/不能吃" */
    private List<String> collectDislikes(User user, MealPlan plan) {
        List<String> dislikes = new ArrayList<>(splitTags(plan.getDislikes()));
        String allergy = profileOptionService.labelsOf(ProfileOptionType.ALLERGY, user.getAllergyNote());
        if (allergy != null && !allergy.isBlank()) {
            dislikes.addAll(List.of(allergy.split("[、,，]")));
        }
        return dislikes.stream().map(String::trim).filter(s -> !s.isBlank()).distinct().toList();
    }

    /** 「这一餐还有什么」——替换时给模型一点搭配上的上下文 */
    private String describeMeal(MealPlan plan, MealPlanItem item) {
        String other = itemRepository
                .findByPlanIdAndDayIndexOrderBySortOrderAscIdAsc(plan.getId(), item.getDayIndex()).stream()
                .filter(i -> item.getMealType().equals(i.getMealType()) && !i.getId().equals(item.getId()))
                .map(i -> i.getFoodName() + " " + plain(i.getAmount()) + "g")
                .reduce((a, b) -> a + "、" + b)
                .orElse("（这一餐只有它）");
        return mealLabel(item.getMealType()) + "，同餐还有：" + other;
    }

    private String normalizeGoal(String goal) {
        if (goal == null || goal.isBlank()) {
            return "maintain";
        }
        String normalized = goal.trim().toLowerCase();
        if (!GOAL_LABELS.containsKey(normalized)) {
            throw new BusinessException("不支持的目标类型：" + goal);
        }
        return normalized;
    }

    private int normalizeDays(Integer days) {
        if (days == null) {
            return 7;
        }
        if (!ALLOWED_DAYS.contains(days)) {
            throw new BusinessException("方案周期只能是 3 / 7 / 14 天");
        }
        return days;
    }

    /** 餐次去重并按早餐→晚餐排序，空则给默认三餐 */
    private List<String> normalizeMeals(List<String> meals) {
        if (meals == null || meals.isEmpty()) {
            return List.of("breakfast", "lunch", "dinner");
        }
        LinkedHashSet<String> valid = new LinkedHashSet<>();
        for (String meal : meals) {
            if (meal != null && MEAL_ORDER.contains(meal.trim())) {
                valid.add(meal.trim());
            }
        }
        if (valid.isEmpty()) {
            throw new BusinessException("至少要选择一餐");
        }
        return MEAL_ORDER.stream().filter(valid::contains).toList();
    }

    private List<String> splitMeals(String meals) {
        if (meals == null || meals.isBlank()) {
            return List.of("breakfast", "lunch", "dinner");
        }
        LinkedHashSet<String> valid = new LinkedHashSet<>();
        for (String meal : meals.split(",")) {
            if (MEAL_ORDER.contains(meal.trim())) {
                valid.add(meal.trim());
            }
        }
        return MEAL_ORDER.stream().filter(valid::contains).toList();
    }

    private List<String> splitTags(String joined) {
        if (joined == null || joined.isBlank()) {
            return List.of();
        }
        return List.of(joined.split("[、,，]")).stream().map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private List<String> cleanList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static String mealLabel(String mealType) {
        return MEAL_LABELS.getOrDefault(mealType, mealType);
    }

    private static BigDecimal scale(BigDecimal per100, BigDecimal amount) {
        if (per100 == null || amount == null) {
            return BigDecimal.ZERO;
        }
        return per100.multiply(amount).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(List<MealPlanItemVO> items, java.util.function.Function<MealPlanItemVO, BigDecimal> getter) {
        return items.stream()
                .map(getter)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageDailyCalories(List<MealPlanItem> items, Integer days) {
        if (items.isEmpty() || days == null || days <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = items.stream()
                .map(i -> i.getCalories() != null ? i.getCalories() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP);
    }

    private int countItems(List<MealPlanDayVO> days) {
        return days.stream().mapToInt(d -> d.getMeals().stream().mapToInt(g -> g.getItems().size()).sum()).sum();
    }

    /**
     * 克数 → 好读的份量。
     * 「克/毫升」这类计量单位直接跟在数字后面；「个/份/盒」这类可数单位在有单重时换算成个数。
     */
    private static String amountText(BigDecimal amount, String unitName, BigDecimal unitWeight) {
        if (amount == null) {
            return "--";
        }
        String unit = unitName == null ? "" : unitName.trim();
        if (!unit.isEmpty() && MEASURE_UNITS.contains(unit.toLowerCase())) {
            return plain(amount) + unit;
        }
        if (!unit.isEmpty() && unitWeight != null && unitWeight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal count = amount.divide(unitWeight, 1, RoundingMode.HALF_UP);
            return count.stripTrailingZeros().toPlainString() + unit;
        }
        return plain(amount) + "g";
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static Integer toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : (int) Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal toDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return value == null ? null : new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
