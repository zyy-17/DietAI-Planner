package com.zyyqq.service;

import com.zyyqq.dto.response.FoodOptionVO;
import com.zyyqq.dto.request.SaveCustomRecipeRequest;
import com.zyyqq.entity.Food;
import com.zyyqq.entity.FoodCategory;
import com.zyyqq.entity.UserCustomFood;
import com.zyyqq.entity.UserCustomFoodItem;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.FoodCategoryRepository;
import com.zyyqq.repository.FoodRepository;
import com.zyyqq.repository.UserCustomFoodItemRepository;
import com.zyyqq.repository.UserCustomFoodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户自定义（私有）食物业务。
 * <p>
 * 自定义食物存放在独立的 user_custom_food 表中，<b>不会写入公共食物库（food 表）</b>，
 * 因此食物库列表、食物搜索、食物推荐都不会出现用户自建的食物。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCustomFoodService {

    /** 自定义食物兜底分类名称 */
    private static final String DEFAULT_CATEGORY_NAME = "其他";

    private final UserCustomFoodRepository userCustomFoodRepository;
    private final FoodCategoryRepository foodCategoryRepository;
    private final FoodRepository foodRepository;
    private final UserCustomFoodItemRepository userCustomFoodItemRepository;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /**
     * 记录饮食时创建/复用用户的私有自定义食物。
     * 同名食物会被复用并刷新营养数据，避免同一用户重复堆积同名条目。
     */
    @Transactional
    public UserCustomFood findOrCreate(Long userId, String name, Long categoryId, BigDecimal calories,
                                       BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat,
                                       BigDecimal fiber, String unitName, BigDecimal unitWeight) {
        String foodName = name == null ? "" : name.trim();
        if (foodName.isEmpty()) {
            throw new BusinessException("食物名称不能为空");
        }
        Optional<UserCustomFood> existing = userCustomFoodRepository.findFirstByUserIdAndName(userId, foodName);
        if (existing.isPresent()) {
            UserCustomFood food = existing.get();
            food.setCalories(calories);
            food.setProtein(protein);
            food.setCarbohydrate(carbohydrate);
            food.setFat(fat);
            if (fiber != null) {
                food.setFiber(fiber);
            }
            if (categoryId != null) {
                food.setCategoryId(categoryId);
            }
            applyUnit(food, unitName, unitWeight);
            return userCustomFoodRepository.save(food);
        }
        UserCustomFood food = UserCustomFood.builder()
                .userId(userId)
                .name(foodName)
                .categoryId(resolveCategoryId(categoryId))
                .calories(calories)
                .protein(protein)
                .carbohydrate(carbohydrate)
                .fat(fat)
                .fiber(fiber)
                .build();
        applyUnit(food, unitName, unitWeight);
        UserCustomFood saved = userCustomFoodRepository.save(food);
        log.info("创建用户自定义食物: userId={}, name={}", userId, saved.getName());
        return saved;
    }

    /** 单位名为空时视为不使用单位计数，一并清掉参考克重 */
    private void applyUnit(UserCustomFood food, String unitName, BigDecimal unitWeight) {
        String name = unitName == null ? null : unitName.trim();
        if (name == null || name.isEmpty()) {
            food.setUnitName(null);
            food.setUnitWeight(null);
            return;
        }
        food.setUnitName(name);
        food.setUnitWeight(unitWeight != null && unitWeight.compareTo(BigDecimal.ZERO) > 0 ? unitWeight : null);
    }

    /** 当前用户的自定义（私有）食物列表 */
    public List<UserCustomFood> listMine(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userCustomFoodRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** 当前用户的自定义食物，转换为统一的食物选择项结构 */
    public List<FoodOptionVO> listMineAsOptions(Long userId) {
        return listMine(userId).stream().map(FoodOptionVO::fromCustom).toList();
    }

    /** 取本人名下的自定义食物，不存在或不属于该用户时抛业务异常 */
    public UserCustomFood getOwned(Long id, Long userId) {
        return userCustomFoodRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException("自定义食物不存在或不属于当前用户"));
    }

    /**
     * 编辑本人名下的自定义食物（仅本人可改）。
     * 组合食谱（isRecipe=true）不允许直接改营养值（营养由食材累加），
     * 只允许改名称与分类。
     */
    @Transactional
    public UserCustomFood update(Long id, Long userId, String name, Long categoryId,
                                 BigDecimal calories, BigDecimal protein, BigDecimal carbohydrate,
                                 BigDecimal fat, BigDecimal fiber, String unitName, BigDecimal unitWeight) {
        UserCustomFood food = getOwned(id, userId);
        if (name != null && !name.trim().isEmpty()) {
            food.setName(name.trim());
        }
        if (categoryId != null) {
            food.setCategoryId(resolveCategoryId(categoryId));
        }
        if (!Boolean.TRUE.equals(food.getIsRecipe())) {
            if (calories != null) {
                food.setCalories(calories);
            }
            if (protein != null) {
                food.setProtein(protein);
            }
            if (carbohydrate != null) {
                food.setCarbohydrate(carbohydrate);
            }
            if (fat != null) {
                food.setFat(fat);
            }
            if (fiber != null) {
                food.setFiber(fiber);
            }
            applyUnit(food, unitName, unitWeight);
        }
        UserCustomFood saved = userCustomFoodRepository.save(food);
        log.info("编辑自定义食物: userId={}, id={}, name={}", userId, id, saved.getName());
        return saved;
    }

    /** 删除本人名下的自定义食物（连带其组成明细） */
    @Transactional
    public void delete(Long id, Long userId) {
        UserCustomFood food = getOwned(id, userId);
        if (Boolean.TRUE.equals(food.getIsRecipe())) {
            userCustomFoodItemRepository.deleteByRecipeId(id);
        }
        userCustomFoodRepository.delete(food);
        log.info("删除自定义食物: userId={}, id={}, name={}", userId, id, food.getName());
    }

    /** 批量查询自定义食物 ID 与名称的映射 */
    public Map<Long, String> getNamesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> nameMap = new HashMap<>();
        for (Object[] row : userCustomFoodRepository.findIdAndNameByIds(ids)) {
            nameMap.put((Long) row[0], (String) row[1]);
        }
        return nameMap;
    }

    /** 解析食物分类ID，未指定时使用"其他"或第一个可用分类兜底 */
    private Long resolveCategoryId(Long categoryId) {
        if (categoryId != null) {
            return categoryId;
        }
        List<FoodCategory> categories = foodCategoryRepository.findAllEnabled();
        if (categories.isEmpty()) {
            // 极端情况下没有分类，使用 1 兜底以保证外键有效
            return 1L;
        }
        return categories.stream()
                .filter(c -> DEFAULT_CATEGORY_NAME.equals(c.getName()))
                .map(FoodCategory::getId)
                .findFirst()
                .orElse(categories.get(0).getId());
    }

    /**
     * 创建（或同名覆盖）用户自定义食谱：由若干基础食材组合，营养值自动累加。
     * <p>同名食谱会被复用并重建明细，避免同一用户重复堆积同名食谱。</p>
     */
    @Transactional
    public UserCustomFood saveRecipe(Long userId, SaveCustomRecipeRequest request) {
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw new BusinessException("食谱名称不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("至少添加一种食材");
        }

        // 累计营养值（每 100g × 克数 ÷ 100）
        BigDecimal totalCal = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalCarb = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;
        BigDecimal totalFiber = BigDecimal.ZERO;

        List<UserCustomFoodItem> items = new ArrayList<>();
        for (SaveCustomRecipeRequest.Item item : request.getItems()) {
            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("食材用量必须大于 0");
            }
            Food food = foodRepository.findById(item.getFoodId())
                    .orElseThrow(() -> new BusinessException("食材不存在或已删除"));
            BigDecimal ratio = item.getAmount().divide(HUNDRED, 4, RoundingMode.HALF_UP);
            BigDecimal cal = mul(food.getCalories(), ratio);
            BigDecimal pro = mul(food.getProtein(), ratio);
            BigDecimal carb = mul(food.getCarbohydrate(), ratio);
            BigDecimal fat = mul(food.getFat(), ratio);
            BigDecimal fiber = mul(food.getFiber(), ratio);

            totalCal = totalCal.add(cal);
            totalProtein = totalProtein.add(pro);
            totalCarb = totalCarb.add(carb);
            totalFat = totalFat.add(fat);
            totalFiber = totalFiber.add(fiber);

            items.add(UserCustomFoodItem.builder()
                    .foodId(food.getId())
                    .foodName(food.getName())
                    .amount(item.getAmount())
                    .calories(cal)
                    .protein(pro)
                    .carbohydrate(carb)
                    .fat(fat)
                    .build());
        }

        // 累加后的营养是「整道食谱」总量，需要折算回每 100g 以便复用 findOrCreate 的换算逻辑
        // 这里直接用食物总量作为基准：先算出食谱总克数，再归一化到每 100g。
        BigDecimal totalWeight = request.getItems().stream()
                .map(SaveCustomRecipeRequest.Item::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("食材总用量必须大于 0");
        }
        BigDecimal norm = totalWeight.divide(HUNDRED, 4, RoundingMode.HALF_UP);
        BigDecimal per100Cal = totalCal.divide(norm, 2, RoundingMode.HALF_UP);
        BigDecimal per100Protein = totalProtein.divide(norm, 2, RoundingMode.HALF_UP);
        BigDecimal per100Carb = totalCarb.divide(norm, 2, RoundingMode.HALF_UP);
        BigDecimal per100Fat = totalFat.divide(norm, 2, RoundingMode.HALF_UP);
        BigDecimal per100Fiber = totalFiber.divide(norm, 2, RoundingMode.HALF_UP);

        // 复用/新建食谱主体
        UserCustomFood recipe = userCustomFoodRepository.findFirstByUserIdAndName(userId, name)
                .orElseGet(() -> UserCustomFood.builder()
                        .userId(userId)
                        .name(name)
                        .categoryId(resolveCategoryId(request.getCategoryId()))
                        .build());
        recipe.setCategoryId(resolveCategoryId(request.getCategoryId()));
        recipe.setCalories(per100Cal);
        recipe.setProtein(per100Protein);
        recipe.setCarbohydrate(per100Carb);
        recipe.setFat(per100Fat);
        recipe.setFiber(per100Fiber);
        recipe.setIsRecipe(true);
        // 组合食谱按「整份」记录，默认一份即总克数
        recipe.setUnitName("份");
        recipe.setUnitWeight(totalWeight.setScale(0, RoundingMode.HALF_UP));
        UserCustomFood saved = userCustomFoodRepository.save(recipe);

        // 重建明细
        userCustomFoodItemRepository.deleteByRecipeId(saved.getId());
        for (UserCustomFoodItem it : items) {
            it.setRecipeId(saved.getId());
        }
        userCustomFoodItemRepository.saveAll(items);

        log.info("创建/更新自定义食谱: userId={}, name={}, items={}, totalWeight={}g",
                userId, saved.getName(), items.size(), totalWeight);
        return saved;
    }

    /** 取某食谱的组成明细（仅本人可见的食谱） */
    public List<UserCustomFoodItem> getRecipeItems(Long recipeId, Long userId) {
        UserCustomFood recipe = userCustomFoodRepository.findByIdAndUserId(recipeId, userId)
                .orElseThrow(() -> new BusinessException("食谱不存在或不属于当前用户"));
        if (!Boolean.TRUE.equals(recipe.getIsRecipe())) {
            return List.of();
        }
        return userCustomFoodItemRepository.findByRecipeIdOrderByIdAsc(recipeId);
    }

    /** 克数换算（每 100g 值 × 克数 ÷ 100），null 按 0 处理 */
    private BigDecimal mul(BigDecimal per100, BigDecimal ratio) {
        BigDecimal v = per100 == null ? BigDecimal.ZERO : per100;
        return v.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }
}
