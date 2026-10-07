package com.zyyqq.service;

import com.zyyqq.dto.response.FoodOptionVO;
import com.zyyqq.entity.FoodCategory;
import com.zyyqq.entity.UserCustomFood;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.FoodCategoryRepository;
import com.zyyqq.repository.UserCustomFoodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
}
