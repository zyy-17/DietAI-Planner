package com.zyyqq.service;

import com.zyyqq.dto.request.MealPlanCopyDayRequest;
import com.zyyqq.dto.request.MealPlanItemAddRequest;
import com.zyyqq.dto.request.MealPlanItemReplaceRequest;
import com.zyyqq.dto.request.MealPlanPublishRequest;
import com.zyyqq.dto.request.MealPlanSaveRequest;
import com.zyyqq.dto.response.FoodAlternativeVO;
import com.zyyqq.dto.response.MealGroupVO;
import com.zyyqq.dto.response.MealPlanAlternativesVO;
import com.zyyqq.dto.response.MealPlanCurrentVO;
import com.zyyqq.dto.response.MealPlanDayVO;
import com.zyyqq.dto.response.MealPlanDetailVO;
import com.zyyqq.dto.response.MealPlanItemVO;
import com.zyyqq.dto.response.MealPlanVO;
import com.zyyqq.dto.response.MealProgressVO;
import com.zyyqq.entity.AiGenerationLog;
import com.zyyqq.entity.DietRecord;
import com.zyyqq.entity.Food;
import com.zyyqq.entity.MealPlan;
import com.zyyqq.entity.MealPlanFavorite;
import com.zyyqq.entity.MealPlanItem;
import com.zyyqq.entity.ProfileOptionType;
import com.zyyqq.entity.User;
import com.zyyqq.entity.UserCustomFood;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.AiGenerationLogRepository;
import com.zyyqq.repository.MealPlanFavoriteRepository;
import com.zyyqq.repository.MealPlanItemRepository;
import com.zyyqq.repository.MealPlanRepository;
import com.zyyqq.repository.UserCustomFoodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 食谱（膳食方案）：<b>用户自己写 → 应用执行 / 发布到广场</b>。
 *
 * <p>几个刻意的设计：</p>
 * <ul>
 *   <li><b>营养值不由模型算</b>：用户从食物库选食物填克数，热量与三大营养素一律按
 *       食物库每 100g 的值换算。AI 只在「替换」里做候选排序，且是可选的。</li>
 *   <li><b>今日进度不存状态位</b>：直接看今天 {@code diet_record} 里有没有对应餐次，
 *       避免"食谱说吃了、记录里没有"的两套账。</li>
 *   <li><b>已上架的食谱被改动要重新审核</b>：否则审核过的内容可以被悄悄换成别的，
 *       广场就成了绕过审核的后门。</li>
 *   <li><b>缩短天数不删数据</b>：改回长天数时原来的安排还在，只是暂时不显示、
 *       也不计入统计。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MealPlanService {

    /** 允许的周期 */
    private static final List<Integer> ALLOWED_DAYS = List.of(3, 7, 14);

    /** 默认餐次顺序，也是展示顺序 */
    private static final List<String> MEAL_ORDER = List.of("breakfast", "lunch", "dinner", "snack");

    private static final Map<String, String> MEAL_LABELS = Map.of(
            "breakfast", "早餐", "lunch", "午餐", "dinner", "晚餐", "snack", "加餐");

    private static final Map<String, String> GOAL_LABELS = Map.of(
            "lose", "减脂", "gain", "增重", "maintain", "维持体重");

    private static final Map<String, String> STATUS_LABELS = Map.of(
            "idle", "未执行", "active", "执行中", "archived", "已归档");

    private static final Map<String, String> PUBLISH_LABELS = Map.of(
            "none", "未发布", "pending", "待审核", "approved", "已上架", "rejected", "未通过");

    /** AI 替换时给模型挑的候选数量 */
    private static final int REPLACE_CANDIDATES = 12;

    /** 手动替换弹窗里直接列出的候选数量 */
    private static final int MANUAL_CANDIDATES = 8;

    /** 按「克/毫升」计量的单位，不参与"1个"这种个数换算 */
    private static final List<String> MEASURE_UNITS = List.of("克", "g", "毫升", "ml", "g/ml");

    private static final Pattern DETAIL_PATTERN = Pattern.compile("\"detail\"\\s*:\\s*\"([^\"]*)\"");

    /** 广场一页最多返回多少条 */
    private static final int SQUARE_MAX_PAGE_SIZE = 24;

    private final MealPlanRepository planRepository;
    private final MealPlanItemRepository itemRepository;
    private final MealPlanFavoriteRepository favoriteRepository;
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
    //  我的食谱：新建 / 改信息 / 增删食物
    // ══════════════════════════════════════════════════════════════

    /** 新建一份空食谱，落到「我的食谱」里（status=idle，还没执行）。 */
    @Transactional
    public MealPlanVO create(Long userId, MealPlanSaveRequest request) {
        String goal = normalizeGoal(request == null ? null : request.getGoal());
        int days = normalizeDays(request == null ? null : request.getDays());
        List<String> meals = normalizeMeals(request == null ? null : request.getMeals());

        String name = request == null ? null : request.getName();
        if (name == null || name.isBlank()) {
            name = days + "天" + GOAL_LABELS.get(goal) + "食谱";
        }

        MealPlan plan = MealPlan.builder()
                .userId(userId)
                .name(name.trim())
                .goal(goal)
                .days(days)
                .meals(String.join(",", meals))
                .dailyCalories(request == null ? null : request.getDailyCalories())
                .summary(request == null ? null : trimTo(request.getSummary(), 500))
                .status(MealPlan.STATUS_IDLE)
                .publishStatus(MealPlan.PUBLISH_NONE)
                .build();
        planRepository.save(plan);
        log.info("新建食谱: userId={}, planId={}, days={}, meals={}", userId, plan.getId(), days, meals);
        return toPlanVO(plan, userId, null, false);
    }

    /** 改食谱基本信息（名称 / 目标 / 周期 / 餐次 / 目标热量）。 */
    @Transactional
    public MealPlanVO updateBasic(Long userId, Long planId, MealPlanSaveRequest request) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        if (request.getName() != null && !request.getName().isBlank()) {
            plan.setName(trimTo(request.getName().trim(), 100));
        }
        if (request.getGoal() != null && !request.getGoal().isBlank()) {
            plan.setGoal(normalizeGoal(request.getGoal()));
        }
        if (request.getDays() != null) {
            plan.setDays(normalizeDays(request.getDays()));
        }
        if (request.getMeals() != null && !request.getMeals().isEmpty()) {
            plan.setMeals(String.join(",", normalizeMeals(request.getMeals())));
        }
        if (request.getDailyCalories() != null) {
            plan.setDailyCalories(request.getDailyCalories());
        }
        if (request.getSummary() != null) {
            plan.setSummary(trimTo(request.getSummary(), 500));
        }
        markEdited(plan);
        planRepository.save(plan);
        return withStats(plan, userId, null, false);
    }

    /** 往某天某餐加一条食物，返回这一天的最新明细。 */
    @Transactional
    public MealPlanDayVO addItem(Long userId, Long planId, MealPlanItemAddRequest request) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        int dayIndex = requireDayIndex(plan, request.getDayIndex());
        String mealType = requireMealType(plan, request.getMealType());
        BigDecimal amount = requireAmount(request.getAmount());

        FoodPool pool = loadFoodPool(userId);
        FoodRef ref = resolveFoodRef(pool, request.getFoodId(), request.getFoodSource(), null);

        List<MealPlanItem> exist = itemRepository
                .findByPlanIdAndDayIndexOrderBySortOrderAscIdAsc(planId, dayIndex);
        int nextOrder = exist.stream()
                .filter(i -> mealType.equals(i.getMealType()))
                .mapToInt(i -> i.getSortOrder() == null ? 0 : i.getSortOrder())
                .max().orElse(-1) + 1;

        itemRepository.save(buildItem(planId, dayIndex, mealType, ref, amount, nextOrder));
        markEdited(plan);
        planRepository.save(plan);
        log.info("食谱加食物: planId={}, day={}, meal={}, {} {}g", planId, dayIndex, mealType, ref.name(), amount);
        return buildDayVO(dayIndex, itemRepository.findByPlanIdAndDayIndexOrderBySortOrderAscIdAsc(planId, dayIndex),
                splitMeals(plan.getMeals()));
    }

    /**
     * 改一条食物：<b>只给克数就是改量</b>；带上 foodId/foodSource（或 foodName）就是换成另一种食物。
     * 两种情况都要按食物库重新换算营养值。
     */
    @Transactional
    public MealPlanDayVO updateItem(Long userId, Long planId, Long itemId, MealPlanItemReplaceRequest request) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        MealPlanItem item = requireItem(plan, itemId);

        boolean changeFood = request != null
                && ((request.getFoodId() != null)
                || (request.getFoodName() != null && !request.getFoodName().isBlank()));
        BigDecimal amount = request == null ? null : request.getAmount();
        if (amount == null) {
            amount = item.getAmount();
        }
        amount = requireAmount(amount);

        FoodPool pool = loadFoodPool(userId);
        FoodRef ref = changeFood
                ? resolveFoodRef(pool, request.getFoodId(), request.getFoodSource(), request.getFoodName())
                : resolveFoodRef(pool, item.getFoodId(), item.getFoodSource(), item.getFoodName());

        String before = item.getFoodName();
        applyFoodRef(item, ref, amount);
        itemRepository.save(item);
        markEdited(plan);
        planRepository.save(plan);
        if (changeFood && !before.equals(ref.name())) {
            log.info("替换食谱食物: planId={}, itemId={}, {} -> {}", planId, itemId, before, ref.name());
        }

        return buildDayVO(item.getDayIndex(),
                itemRepository.findByPlanIdAndDayIndexOrderBySortOrderAscIdAsc(planId, item.getDayIndex()),
                splitMeals(plan.getMeals()));
    }

    /** 移除一条食物。 */
    @Transactional
    public MealPlanDayVO removeItem(Long userId, Long planId, Long itemId) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        MealPlanItem item = requireItem(plan, itemId);
        Integer dayIndex = item.getDayIndex();
        itemRepository.delete(item);
        markEdited(plan);
        planRepository.save(plan);
        log.info("食谱移除食物: planId={}, itemId={}", planId, itemId);
        return buildDayVO(dayIndex,
                itemRepository.findByPlanIdAndDayIndexOrderBySortOrderAscIdAsc(planId, dayIndex),
                splitMeals(plan.getMeals()));
    }

    /** 把某一天的安排复制到另外几天（可覆盖）。返回被改动的那些天。 */
    @Transactional
    public List<MealPlanDayVO> copyDay(Long userId, Long planId, MealPlanCopyDayRequest request) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        int fromDay = requireDayIndex(plan, request.getFromDayIndex());
        if (request.getToDayIndexes() == null || request.getToDayIndexes().isEmpty()) {
            throw new BusinessException("请选择要复制到哪几天");
        }
        boolean overwrite = Boolean.TRUE.equals(request.getOverwrite());

        List<MealPlanItem> source = itemRepository
                .findByPlanIdAndDayIndexOrderBySortOrderAscIdAsc(planId, fromDay);
        if (source.isEmpty()) {
            throw new BusinessException("第 " + fromDay + " 天还没有安排，没有可复制的内容");
        }

        List<String> meals = splitMeals(plan.getMeals());
        List<MealPlanDayVO> changed = new ArrayList<>();
        Set<Integer> targets = new LinkedHashSet<>();
        for (Integer raw : request.getToDayIndexes()) {
            int toDay = requireDayIndex(plan, raw);
            if (toDay == fromDay || !targets.add(toDay)) {
                continue;
            }
            List<MealPlanItem> exist = itemRepository
                    .findByPlanIdAndDayIndexOrderBySortOrderAscIdAsc(planId, toDay);
            if (overwrite) {
                itemRepository.deleteAll(exist);
                exist = List.of();
            }
            // 只补目标日期里还没有的「餐次 + 食物名」，避免重复叠加
            Set<String> occupied = new LinkedHashSet<>();
            for (MealPlanItem item : exist) {
                occupied.add(item.getMealType() + "|" + item.getFoodName());
            }
            for (MealPlanItem item : source) {
                if (!occupied.add(item.getMealType() + "|" + item.getFoodName())) {
                    continue;
                }
                itemRepository.save(buildItem(planId, toDay, item.getMealType(),
                        item.getFoodName(), item.getImageUrl(), item.getFoodId(), item.getFoodSource(),
                        item.getAmount(), item.getUnitName(), item.getUnitWeight(),
                        item.getCalories(), item.getProtein(), item.getCarbohydrate(), item.getFat(),
                        item.getSortOrder() == null ? 0 : item.getSortOrder()));
            }
            changed.add(buildDayVO(toDay,
                    itemRepository.findByPlanIdAndDayIndexOrderBySortOrderAscIdAsc(planId, toDay), meals));
        }
        if (targets.isEmpty()) {
            throw new BusinessException("没有需要复制的日期");
        }
        markEdited(plan);
        planRepository.save(plan);
        log.info("食谱复制日期: planId={}, from={}, to={}, overwrite={}", planId, fromDay, targets, overwrite);
        return changed;
    }

    /** 把食谱设为「正在执行」；同一用户同时只有一份执行中的方案。 */
    @Transactional
    public MealPlanVO apply(Long userId, Long planId) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        List<MealPlanItem> items = itemRepository.findByPlanIdOrderByDayIndexAscSortOrderAscIdAsc(planId);
        if (items.isEmpty()) {
            throw new BusinessException("食谱里还没有食物，先加几样再开始");
        }
        for (MealPlan other : planRepository.findByUserIdAndStatus(userId, MealPlan.STATUS_ACTIVE)) {
            if (!other.getId().equals(planId)) {
                other.setStatus(MealPlan.STATUS_ARCHIVED);
                planRepository.save(other);
            }
        }
        plan.setStatus(MealPlan.STATUS_ACTIVE);
        plan.setStartDate(LocalDate.now());
        planRepository.save(plan);
        log.info("应用食谱开始执行: userId={}, planId={}", userId, planId);
        return withStats(plan, userId, null, false);
    }

    // ══════════════════════════════════════════════════════════════
    //  我的食谱：查询 / 删除
    // ══════════════════════════════════════════════════════════════

    /** 当前执行中的食谱 + 今天吃什么 + 今日进度；没有则 plan 为 null。 */
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

        MealPlanVO planVO = withStats(plan, userId, null, false);

        return MealPlanCurrentVO.builder()
                .plan(planVO)
                .today(buildDayVO(currentDay, todayItems, meals))
                .currentDayIndex(currentDay)
                .todayDate(LocalDate.now())
                .progress(progress)
                .todayIntakeCalories(intake)
                .build();
    }

    /**
     * 我的食谱列表。
     *
     * @param status null 或 "all" 取全部；"recipes" 只要我的食谱（不含已归档的历史）；
     *               也可以传 idle / active / archived 精确过滤
     */
    public List<MealPlanVO> list(Long userId, String status) {
        List<MealPlan> plans;
        if (status == null || status.isBlank() || "all".equals(status)) {
            plans = planRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } else if ("recipes".equals(status)) {
            plans = planRepository.findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, MealPlan.STATUS_ARCHIVED);
        } else {
            plans = planRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        }
        return decorateList(plans, userId, Set.of());
    }

    /** 食谱详情：逐天明细。 */
    public MealPlanDetailVO detail(Long userId, Long planId) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        return buildDetail(plan, userId, false);
    }

    /** 删除食谱（连同条目与收藏关系）。 */
    @Transactional
    public void delete(Long userId, Long planId) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        itemRepository.deleteByPlanId(plan.getId());
        favoriteRepository.deleteByPlanId(plan.getId());
        planRepository.delete(plan);
        log.info("删除食谱: userId={}, planId={}", userId, planId);
    }

    // ══════════════════════════════════════════════════════════════
    //  发布 / 下架
    // ══════════════════════════════════════════════════════════════

    /** 更新封面图地址（封面上传接口调它）。 */
    @Transactional
    public void updateCover(Long userId, Long planId, String coverUrl) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        plan.setCoverUrl(trimTo(coverUrl, 255));
        markEdited(plan);
        planRepository.save(plan);
    }

    /** 提交发布到广场，进入待审核。 */
    @Transactional
    public MealPlanVO publish(Long userId, Long planId, MealPlanPublishRequest request) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        long itemCount = itemRepository.countByPlanId(planId);
        if (itemCount == 0) {
            throw new BusinessException("食谱里还没有食物，先加几样再发布");
        }
        if (MealPlan.PUBLISH_PENDING.equals(plan.getPublishStatus())) {
            throw new BusinessException("这份食谱正在审核中，请等待管理员处理");
        }
        if (request != null) {
            if (request.getCoverUrl() != null) {
                plan.setCoverUrl(trimTo(request.getCoverUrl(), 255));
            }
            if (request.getTags() != null) {
                plan.setTags(trimTo(String.join("、", cleanList(request.getTags())), 200));
            }
            if (request.getDifficulty() != null) {
                plan.setDifficulty(trimTo(request.getDifficulty().trim(), 20));
            }
            if (request.getExpectedLoss() != null) {
                plan.setExpectedLoss(trimTo(request.getExpectedLoss().trim(), 30));
            }
            if (request.getDescription() != null) {
                plan.setDescription(trimTo(request.getDescription(), 1000));
            }
            if (request.getSummary() != null) {
                plan.setSummary(trimTo(request.getSummary(), 500));
            }
        }
        plan.setPublishStatus(MealPlan.PUBLISH_PENDING);
        plan.setRejectReason(null);
        planRepository.save(plan);
        log.info("食谱提交审核: userId={}, planId={}", userId, planId);
        return withStats(plan, userId, null, false);
    }

    /** 作者下架 / 撤回审核。 */
    @Transactional
    public MealPlanVO unpublish(Long userId, Long planId) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        plan.setPublishStatus(MealPlan.PUBLISH_NONE);
        plan.setPublishedAt(null);
        plan.setRejectReason(null);
        planRepository.save(plan);
        log.info("食谱下架: userId={}, planId={}", userId, planId);
        return withStats(plan, userId, null, false);
    }

    // ══════════════════════════════════════════════════════════════
    //  食谱广场
    // ══════════════════════════════════════════════════════════════

    /** 广场列表：只返回审核通过的，支持热门 / 最新排序与名称搜索。 */
    public Page<MealPlanVO> square(Long userId, String sort, String keyword, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), SQUARE_MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        boolean byHot = !MealPlan.SORT_NEW.equals(sort);
        String kw = keyword == null ? null : keyword.trim();

        Page<MealPlan> plans;
        if (kw != null && !kw.isEmpty()) {
            plans = byHot
                    ? planRepository.findByPublishStatusAndNameContainingOrderByUsageCountDesc(
                            MealPlan.PUBLISH_APPROVED, kw, pageable)
                    : planRepository.findByPublishStatusAndNameContainingOrderByPublishedAtDesc(
                            MealPlan.PUBLISH_APPROVED, kw, pageable);
        } else {
            plans = byHot
                    ? planRepository.findByPublishStatusOrderByUsageCountDescPublishedAtDescIdDesc(
                            MealPlan.PUBLISH_APPROVED, pageable)
                    : planRepository.findByPublishStatusOrderByPublishedAtDescIdDesc(
                            MealPlan.PUBLISH_APPROVED, pageable);
        }

        Set<Long> favorited = favoriteIds(userId, plans.getContent());
        return plans.map(plan -> withStats(plan, userId, maskAuthor(plan.getUserId()), favorited.contains(plan.getId())));
    }

    /** 广场食谱详情（作者也可以查看自己待审核的那份）。 */
    public MealPlanDetailVO squareDetail(Long userId, Long planId) {
        MealPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("食谱不存在"));
        boolean mine = plan.getUserId().equals(userId);
        if (!plan.isOnSquare() && !mine) {
            throw new BusinessException("这份食谱还没有上架");
        }
        return buildDetail(plan, userId, true);
    }

    /** 收藏 / 取消收藏。 */
    @Transactional
    public boolean setFavorite(Long userId, Long planId, boolean favorite) {
        MealPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("食谱不存在"));
        if (!plan.isOnSquare()) {
            throw new BusinessException("这份食谱还没有上架");
        }
        Optional<MealPlanFavorite> exist = favoriteRepository.findByUserIdAndPlanId(userId, planId);
        if (favorite) {
            if (exist.isEmpty()) {
                favoriteRepository.save(MealPlanFavorite.builder().userId(userId).planId(planId).build());
                plan.setFavoriteCount(Math.max(0, nz(plan.getFavoriteCount())) + 1);
                planRepository.save(plan);
            }
            return true;
        }
        if (exist.isPresent()) {
            favoriteRepository.delete(exist.get());
            plan.setFavoriteCount(Math.max(0, nz(plan.getFavoriteCount()) - 1));
            planRepository.save(plan);
        }
        return false;
    }

    /** 我收藏的食谱（仍然只显示在架上的）。 */
    public List<MealPlanVO> myFavorites(Long userId) {
        List<MealPlanFavorite> rows = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(MealPlanFavorite::getPlanId).toList();
        List<MealPlan> plans = planRepository.findByIdInAndPublishStatus(ids, MealPlan.PUBLISH_APPROVED);
        // 按收藏时间倒序还原顺序
        Map<Long, MealPlan> byId = new HashMap<>();
        for (MealPlan plan : plans) {
            byId.put(plan.getId(), plan);
        }
        List<MealPlan> ordered = new ArrayList<>();
        for (Long id : ids) {
            MealPlan plan = byId.get(id);
            if (plan != null) {
                ordered.add(plan);
            }
        }
        Set<Long> favorited = favoriteIds(userId, ordered);
        List<MealPlanVO> result = new ArrayList<>();
        for (MealPlan plan : ordered) {
            result.add(withStats(plan, userId, maskAuthor(plan.getUserId()), favorited.contains(plan.getId())));
        }
        return result;
    }

    /** 「保存为我的食谱」：复制一份到自己账号，原食谱使用人数 +1。 */
    @Transactional
    public MealPlanVO copyToMine(Long userId, Long planId) {
        MealPlan source = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("食谱不存在"));
        if (!source.isOnSquare() && !source.getUserId().equals(userId)) {
            throw new BusinessException("这份食谱还没有上架");
        }
        if (source.getUserId().equals(userId)) {
            throw new BusinessException("这就是你自己的食谱，不用再保存一份");
        }
        List<MealPlanItem> sourceItems = itemRepository
                .findByPlanIdOrderByDayIndexAscSortOrderAscIdAsc(planId);

        MealPlan copy = MealPlan.builder()
                .userId(userId)
                .name(trimTo(source.getName(), 100))
                .goal(source.getGoal())
                .days(source.getDays())
                .meals(source.getMeals())
                .dailyCalories(source.getDailyCalories())
                .summary(source.getSummary())
                .description(source.getDescription())
                .coverUrl(source.getCoverUrl())
                .tags(source.getTags())
                .difficulty(source.getDifficulty())
                .expectedLoss(source.getExpectedLoss())
                .status(MealPlan.STATUS_IDLE)
                .publishStatus(MealPlan.PUBLISH_NONE)
                .sourcePlanId(source.getId())
                .build();
        planRepository.save(copy);

        for (MealPlanItem item : sourceItems) {
            itemRepository.save(buildItem(copy.getId(), item.getDayIndex(), item.getMealType(),
                    item.getFoodName(), item.getImageUrl(), item.getFoodId(), item.getFoodSource(),
                    item.getAmount(), item.getUnitName(), item.getUnitWeight(),
                    item.getCalories(), item.getProtein(), item.getCarbohydrate(), item.getFat(),
                    item.getSortOrder() == null ? 0 : item.getSortOrder()));
        }

        source.setUsageCount(nz(source.getUsageCount()) + 1);
        planRepository.save(source);
        log.info("保存他人食谱为我的: userId={}, sourcePlanId={}, newPlanId={}", userId, planId, copy.getId());
        return withStats(copy, userId, null, false);
    }

    // ══════════════════════════════════════════════════════════════
    //  管理员审核
    // ══════════════════════════════════════════════════════════════

    /** 待审核 / 已处理的食谱分页（管理员）。 */
    public Page<MealPlanVO> adminPage(String publishStatus, int page, int size) {
        String status = (publishStatus == null || publishStatus.isBlank())
                ? MealPlan.PUBLISH_PENDING : publishStatus.trim();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<MealPlan> plans = planRepository.findByPublishStatusOrderByUpdatedAtAscIdAsc(status, pageable);
        return plans.map(plan -> withStats(plan, null, maskAuthor(plan.getUserId()), false));
    }

    /** 管理后台查看指定食谱的完整内容（不受"已上架"限制，待审核的也要能看到） */
    public MealPlanDetailVO adminDetail(Long planId) {
        MealPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("食谱不存在"));
        return buildDetail(plan, null, true);
    }

    /** 审核通过，上架到广场。 */
    @Transactional
    public void approve(Long planId) {
        MealPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("食谱不存在"));
        if (itemRepository.countByPlanId(planId) == 0) {
            throw new BusinessException("这份食谱没有任何食物，不能上架");
        }
        plan.setPublishStatus(MealPlan.PUBLISH_APPROVED);
        plan.setPublishedAt(LocalDateTime.now());
        plan.setRejectReason(null);
        planRepository.save(plan);
        log.info("食谱审核通过: planId={}", planId);
    }

    /** 审核不通过，附上原因。 */
    @Transactional
    public void reject(Long planId, String reason) {
        MealPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException("食谱不存在"));
        plan.setPublishStatus(MealPlan.PUBLISH_REJECTED);
        plan.setPublishedAt(null);
        plan.setRejectReason(trimTo(reason == null ? "" : reason, 500));
        planRepository.save(plan);
        log.info("食谱审核驳回: planId={}, reason={}", planId, reason);
    }

    /** 管理后台首页要用的待审核数量 */
    public long countPending() {
        return planRepository.countByPublishStatus(MealPlan.PUBLISH_PENDING);
    }

    // ══════════════════════════════════════════════════════════════
    //  单项替换
    // ══════════════════════════════════════════════════════════════

    /** 手动替换的可选项：同类目 + 热量接近，不调 AI。 */
    public List<FoodAlternativeVO> itemCandidates(Long userId, Long planId, Long itemId) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        MealPlanItem item = requireItem(plan, itemId);
        User user = userService.getUserById(userId);
        FoodPool pool = loadFoodPool(userId);
        return pickReplaceCandidates(pool, item, collectDislikes(user, plan)).stream()
                .limit(MANUAL_CANDIDATES)
                .map(ref -> buildAlternative(ref, item.getAmount(), ""))
                .toList();
    }

    /**
     * 「AI 智能替换」候选：先从食物库按「同类目 + 热量接近」筛一批，
     * 再让模型排序并给理由。模型不可用时退化为算法排序，并在响应里标明 aiUsed=false。
     */
    public MealPlanAlternativesVO alternatives(Long userId, Long planId, Long itemId, String reason) {
        MealPlan plan = requireOwnedPlan(userId, planId);
        MealPlanItem item = requireItem(plan, itemId);

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
            writeGenerationLog(userId, "替换 " + item.getFoodName(), alternatives.size() + " 个候选");
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
    //  食物库
    // ══════════════════════════════════════════════════════════════

    /** 食物库里的一项，屏蔽 Food 与 UserCustomFood 两张表的差异。 */
    private record FoodRef(Long id, String source, String name, Long categoryId, String imageUrl,
                           BigDecimal calories, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat,
                           String unitName, BigDecimal unitWeight) {
    }

    /**
     * 已加载的食物池。
     * 除精确名索引外，也保留全量列表用于模糊匹配（用户/模型偶尔会写「燕麦片」而库里是「燕麦」）。
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

        /** 按「来源:ID」取，前端回传 foodId + foodSource 时用 */
        FoodRef byId(String source, Long id) {
            if (id == null) {
                return null;
            }
            String key = (source == null || source.isBlank() ? MealPlanItem.SOURCE_SYSTEM : source) + ":" + id;
            return byKey.get(key);
        }
    }

    private FoodPool loadFoodPool(Long userId) {
        List<FoodRef> all = new ArrayList<>();
        Map<String, FoodRef> exact = new LinkedHashMap<>();
        Map<String, FoodRef> byKey = new HashMap<>();
        for (Food food : foodService.getAllApprovedFoods()) {
            FoodRef ref = new FoodRef(food.getId(), MealPlanItem.SOURCE_SYSTEM, food.getName(), food.getCategoryId(),
                    food.getImageUrl(), food.getCalories(), food.getProtein(), food.getCarbohydrate(), food.getFat(),
                    food.getUnitName(), food.getUnitWeight());
            all.add(ref);
            exact.putIfAbsent(ref.name(), ref);
            byKey.put(ref.source() + ":" + ref.id(), ref);
        }
        for (UserCustomFood custom : userCustomFoodRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            FoodRef ref = new FoodRef(custom.getId(), MealPlanItem.SOURCE_USER, custom.getName(), custom.getCategoryId(),
                    null, custom.getCalories(), custom.getProtein(), custom.getCarbohydrate(), custom.getFat(),
                    custom.getUnitName(), custom.getUnitWeight());
            all.add(ref);
            exact.putIfAbsent(ref.name(), ref);
            byKey.put(ref.source() + ":" + ref.id(), ref);
        }
        return new FoodPool(all, exact, byKey);
    }

    /** 拼成「名称|每100g热量|蛋白/碳水/脂肪」一行，给模型一个可核对的数据源。 */
    private static String formatFoodLine(FoodRef ref) {
        return String.format("%s|%s|%s/%s/%s", ref.name(),
                plain(ref.calories()), plain(ref.protein()), plain(ref.carbohydrate()), plain(ref.fat()));
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    /** 挑替换候选：优先同分类，不足时放宽到全部；排除原食物与忌口，再按热量接近度排序。 */
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

    /** 按 id 优先、名称兜底，解析出食物库里的一条。 */
    private FoodRef resolveFoodRef(FoodPool pool, Long foodId, String foodSource, String foodName) {
        FoodRef ref = pool.byId(foodSource, foodId);
        if (ref == null && foodName != null && !foodName.isBlank()) {
            ref = pool.match(foodName);
        }
        if (ref == null) {
            throw new BusinessException("食物库里找不到这项食物，请从食物库重新选择");
        }
        return ref;
    }

    // ══════════════════════════════════════════════════════════════
    //  组装 VO
    // ══════════════════════════════════════════════════════════════

    private MealPlanItem buildItem(Long planId, Integer dayIndex, String mealType,
                                   FoodRef ref, BigDecimal amount, int sortOrder) {
        return buildItem(planId, dayIndex, mealType, ref.name(), ref.imageUrl(), ref.id(), ref.source(),
                amount, ref.unitName(), ref.unitWeight(),
                scale(ref.calories(), amount), scale(ref.protein(), amount),
                scale(ref.carbohydrate(), amount), scale(ref.fat(), amount), sortOrder);
    }

    /** 复制条目时用：营养值直接沿用快照，不再回查食物库（食物被删也不影响已保存的安排）。 */
    private MealPlanItem buildItem(Long planId, Integer dayIndex, String mealType, String foodName, String imageUrl,
                                   Long foodId, String foodSource, BigDecimal amount, String unitName,
                                   BigDecimal unitWeight, BigDecimal calories, BigDecimal protein,
                                   BigDecimal carbohydrate, BigDecimal fat, int sortOrder) {
        return MealPlanItem.builder()
                .planId(planId)
                .dayIndex(dayIndex)
                .mealType(mealType)
                .foodId(foodId)
                .foodSource(foodSource == null ? MealPlanItem.SOURCE_SYSTEM : foodSource)
                .foodName(foodName)
                .imageUrl(imageUrl)
                .unitName(unitName)
                .unitWeight(unitWeight)
                .amount(amount)
                .calories(nzDecimal(calories))
                .protein(nzDecimal(protein))
                .carbohydrate(nzDecimal(carbohydrate))
                .fat(nzDecimal(fat))
                .sortOrder(sortOrder)
                .build();
    }

    /** 把条目切换成另一种食物，并按新食物重算营养值。 */
    private void applyFoodRef(MealPlanItem item, FoodRef ref, BigDecimal amount) {
        item.setFoodId(ref.id());
        item.setFoodSource(ref.source());
        item.setFoodName(ref.name());
        item.setImageUrl(ref.imageUrl());
        item.setUnitName(ref.unitName());
        item.setUnitWeight(ref.unitWeight());
        item.setAmount(amount);
        item.setCalories(scale(ref.calories(), amount));
        item.setProtein(scale(ref.protein(), amount));
        item.setCarbohydrate(scale(ref.carbohydrate(), amount));
        item.setFat(scale(ref.fat(), amount));
    }

    private MealPlanItemVO toItemVO(MealPlanItem item) {
        return MealPlanItemVO.builder()
                .id(item.getId())
                .foodId(item.getFoodId())
                .foodSource(item.getFoodSource())
                .foodName(item.getFoodName())
                .imageUrl(item.getImageUrl())
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

    private MealPlanVO toPlanVO(MealPlan plan, Long viewerId, String authorName, boolean favorited) {
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
                .statusLabel(STATUS_LABELS.getOrDefault(plan.getStatus(), plan.getStatus()))
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .currentDayIndex(resolveCurrentDayIndex(plan))
                .coverUrl(plan.getCoverUrl())
                .tags(splitTags(plan.getTags()))
                .difficulty(plan.getDifficulty())
                .expectedLoss(plan.getExpectedLoss())
                .description(plan.getDescription())
                .publishStatus(plan.getPublishStatus())
                .publishStatusLabel(PUBLISH_LABELS.getOrDefault(plan.getPublishStatus(), plan.getPublishStatus()))
                .rejectReason(plan.getRejectReason())
                .usageCount(nz(plan.getUsageCount()))
                .favoriteCount(nz(plan.getFavoriteCount()))
                .publishedAt(plan.getPublishedAt())
                .authorName(authorName)
                .mine(viewerId != null && viewerId.equals(plan.getUserId()))
                .favorited(favorited)
                .onSquare(plan.isOnSquare())
                .sourcePlanId(plan.getSourcePlanId())
                .build();
    }

    /** 补上条目数与平均每日热量（要查条目表，列表里批量做）。 */
    private MealPlanVO withStats(MealPlan plan, Long viewerId, String authorName, boolean favorited) {
        MealPlanVO vo = toPlanVO(plan, viewerId, authorName, favorited);
        List<MealPlanItem> items = itemRepository
                .findByPlanIdOrderByDayIndexAscSortOrderAscIdAsc(plan.getId());
        vo.setItemCount(items.size());
        vo.setPlannedDailyCalories(averageDailyCalories(items, plan.getDays()));
        return vo;
    }

    private List<MealPlanVO> decorateList(List<MealPlan> plans, Long viewerId, Set<Long> favoriteIds) {
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
            MealPlanVO vo = toPlanVO(plan, viewerId, null, favoriteIds.contains(plan.getId()));
            vo.setItemCount(items.size());
            vo.setPlannedDailyCalories(averageDailyCalories(items, plan.getDays()));
            result.add(vo);
        }
        return result;
    }

    private MealPlanDetailVO buildDetail(MealPlan plan, Long viewerId, boolean needAuthor) {
        List<String> meals = splitMeals(plan.getMeals());
        List<MealPlanItem> all = itemRepository.findByPlanIdOrderByDayIndexAscSortOrderAscIdAsc(plan.getId());

        Map<Integer, List<MealPlanItem>> byDay = new LinkedHashMap<>();
        for (MealPlanItem item : all) {
            byDay.computeIfAbsent(item.getDayIndex() == null ? 1 : item.getDayIndex(), k -> new ArrayList<>()).add(item);
        }
        List<MealPlanDayVO> planDays = new ArrayList<>();
        int days = plan.getDays() == null || plan.getDays() < 1 ? 1 : plan.getDays();
        for (int day = 1; day <= days; day++) {
            planDays.add(buildDayVO(day, byDay.getOrDefault(day, List.of()), meals));
        }

        boolean favorited = viewerId != null
                && favoriteRepository.findByUserIdAndPlanId(viewerId, plan.getId()).isPresent();
        MealPlanVO vo = toPlanVO(plan, viewerId, needAuthor ? maskAuthor(plan.getUserId()) : null, favorited);
        vo.setItemCount(all.size());
        vo.setPlannedDailyCalories(averageDailyCalories(all, plan.getDays()));
        return MealPlanDetailVO.builder().plan(vo).planDays(planDays).build();
    }

    private FoodAlternativeVO buildAlternative(FoodRef ref, BigDecimal amount, String reason) {
        return FoodAlternativeVO.builder()
                .foodId(ref.id())
                .foodSource(ref.source())
                .foodName(ref.name())
                .imageUrl(ref.imageUrl())
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
                .orElseThrow(() -> new BusinessException("食谱不存在"));
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException("无权访问这份食谱");
        }
        return plan;
    }

    private MealPlanItem requireItem(MealPlan plan, Long itemId) {
        MealPlanItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("该食物条目不存在"));
        if (!item.getPlanId().equals(plan.getId())) {
            throw new BusinessException("该食物条目不属于这份食谱");
        }
        return item;
    }

    /**
     * 内容被改动后的发布状态处理：
     * 已上架的食谱改完要重新审核（否则审核过的内容可以被悄悄换掉）；
     * 被驳回的食谱改完可以重新提交。
     */
    private void markEdited(MealPlan plan) {
        if (MealPlan.PUBLISH_APPROVED.equals(plan.getPublishStatus())) {
            plan.setPublishStatus(MealPlan.PUBLISH_PENDING);
            plan.setPublishedAt(null);
            log.info("已上架食谱被修改，重新进入待审核: planId={}", plan.getId());
        } else if (MealPlan.PUBLISH_REJECTED.equals(plan.getPublishStatus())) {
            plan.setPublishStatus(MealPlan.PUBLISH_NONE);
            plan.setRejectReason(null);
        }
    }

    /** 今天是食谱第几天；超出周期后停在最后一天，避免详情页定位到空白 */
    private int resolveCurrentDayIndex(MealPlan plan) {
        if (plan.getStartDate() == null || !MealPlan.STATUS_ACTIVE.equals(plan.getStatus())) {
            return 1;
        }
        long offset = ChronoUnit.DAYS.between(plan.getStartDate(), LocalDate.now()) + 1;
        int days = plan.getDays() == null || plan.getDays() < 1 ? 1 : plan.getDays();
        return (int) Math.min(Math.max(offset, 1), days);
    }

    /** 忌口来源有两处：食谱上记的 + 健康档案里的过敏/忌口 */
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

    /** 广场上的作者只露尾号，如「用户**9」，不直接暴露用户名 */
    private String maskAuthor(Long userId) {
        if (userId == null) {
            return "匿名用户";
        }
        try {
            User user = userService.getUserById(userId);
            return maskUsername(user == null ? null : user.getUsername());
        } catch (Exception e) {
            return "匿名用户";
        }
    }

    private static String maskUsername(String username) {
        if (username == null || username.isBlank()) {
            return "匿名用户";
        }
        String name = username.trim();
        if (name.length() <= 1) {
            return "用户";
        }
        int stars = Math.min(Math.max(name.length() - 1, 1), 2);
        return "用户" + "*".repeat(stars) + name.charAt(name.length() - 1);
    }

    /** 批量取「我收藏了哪些」，避免广场列表逐条查库 */
    private Set<Long> favoriteIds(Long userId, List<MealPlan> plans) {
        if (userId == null || plans.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = plans.stream().map(MealPlan::getId).toList();
        return favoriteRepository.findByUserIdAndPlanIdIn(userId, ids).stream()
                .map(MealPlanFavorite::getPlanId)
                .collect(java.util.stream.Collectors.toSet());
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
            throw new BusinessException("食谱周期只能是 3 / 7 / 14 天");
        }
        return days;
    }

    private int requireDayIndex(MealPlan plan, Integer dayIndex) {
        int days = plan.getDays() == null || plan.getDays() < 1 ? 1 : plan.getDays();
        if (dayIndex == null || dayIndex < 1 || dayIndex > days) {
            throw new BusinessException("第 " + dayIndex + " 天超出食谱周期（共 " + days + " 天）");
        }
        return dayIndex;
    }

    private String requireMealType(MealPlan plan, String mealType) {
        if (mealType == null || !MEAL_ORDER.contains(mealType.trim())) {
            throw new BusinessException("餐次不合法");
        }
        String normalized = mealType.trim();
        if (!splitMeals(plan.getMeals()).contains(normalized)) {
            throw new BusinessException("这份食谱没有开启「" + mealLabel(normalized) + "」");
        }
        return normalized;
    }

    private BigDecimal requireAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("克数必须大于 0");
        }
        if (amount.compareTo(new BigDecimal("5000")) > 0) {
            throw new BusinessException("克数看起来不太对，请填 5000 以内");
        }
        return amount;
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

    private static String trimTo(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static String mealLabel(String mealType) {
        return MEAL_LABELS.getOrDefault(mealType, mealType);
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal nzDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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

    /** 平均每日热量只算周期内的那几天，缩短天数后多出来的安排不参与统计 */
    private BigDecimal averageDailyCalories(List<MealPlanItem> items, Integer days) {
        if (items.isEmpty() || days == null || days <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = items.stream()
                .filter(i -> i.getDayIndex() != null && i.getDayIndex() >= 1 && i.getDayIndex() <= days)
                .map(i -> i.getCalories() != null ? i.getCalories() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP);
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
