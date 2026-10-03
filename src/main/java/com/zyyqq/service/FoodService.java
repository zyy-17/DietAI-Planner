package com.zyyqq.service;

import com.zyyqq.dto.request.AddFoodRequest;
import com.zyyqq.entity.Food;
import com.zyyqq.entity.FoodCategory;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.FoodCategoryRepository;
import com.zyyqq.repository.FoodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodService {

    /** 用户自定义食物来源标记 */
    private static final String SOURCE_USER = "user";
    /** 审核通过状态 */
    private static final String STATUS_APPROVED = "approved";
    /** 自定义食物兜底分类名称 */
    private static final String DEFAULT_CATEGORY_NAME = "其他";

    private final FoodRepository foodRepository;
    private final FoodCategoryRepository foodCategoryRepository;

    public Page<Food> getFoods(Long categoryId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (keyword != null && !keyword.isEmpty()) {
            return foodRepository.searchPublicFoods(keyword, STATUS_APPROVED, SOURCE_USER, pageable);
        }
        if (categoryId != null) {
            return foodRepository.findPublicFoodsByCategory(categoryId, STATUS_APPROVED, SOURCE_USER, pageable);
        }
        return foodRepository.findPublicFoods(STATUS_APPROVED, SOURCE_USER, pageable);
    }

    public Food getFoodById(Long id) {
        return foodRepository.findById(id)
                .orElseThrow(() -> new BusinessException("食物不存在"));
    }

    /**
     * 校验食物对该用户是否可见：
     * 系统食物（source != 'user'）所有人可见；用户自定义食物仅创建者本人可见。
     */
    public Food getVisibleFoodForUser(Long foodId, Long userId) {
        Food food = getFoodById(foodId);
        if (SOURCE_USER.equals(food.getSource()) && !Objects.equals(food.getCreatedBy(), userId)) {
            throw new BusinessException("该食物为其他用户的自定义食物，无法查看");
        }
        return food;
    }

    public List<Food> searchFoods(String keyword) {
        return foodRepository.searchPublicApproved(keyword);
    }

    /** 公开食物库（仅系统食物），供内部名称匹配等场景使用 */
    @Cacheable(value = "approvedFoods", key = "'public'")
    public List<Food> getAllApprovedFoods() {
        return foodRepository.findPublicFoodList(STATUS_APPROVED, SOURCE_USER);
    }

    /** 当前用户可用食物：公开食物库 + 本人自定义食物（不含其他用户的自定义食物） */
    @Cacheable(value = "approvedFoods", key = "'user:' + #userId")
    public List<Food> getAvailableFoods(Long userId) {
        List<Food> foods = new ArrayList<>(foodRepository.findPublicFoodList(STATUS_APPROVED, SOURCE_USER));
        if (userId != null) {
            foods.addAll(foodRepository.findByCreatedByAndStatusAndSource(userId, STATUS_APPROVED, SOURCE_USER));
        }
        return foods;
    }

    /** 当前用户的自定义（私有）食物 */
    public List<Food> getMyCustomFoods(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return foodRepository.findByCreatedByAndStatusAndSource(userId, STATUS_APPROVED, SOURCE_USER);
    }

    public Map<Long, String> getFoodNamesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<Object[]> results = foodRepository.findIdAndNameByIds(ids);
        Map<Long, String> nameMap = new HashMap<>();
        for (Object[] row : results) {
            nameMap.put((Long) row[0], (String) row[1]);
        }
        return nameMap;
    }

    /**
     * 记录饮食时创建/复用用户的私有自定义食物。
     * 同名食物会被复用并刷新营养数据，保证不会污染其他用户的公开食物库。
     */
    @Transactional
    @CacheEvict(value = "approvedFoods", allEntries = true)
    public Food findOrCreateCustomFood(Long userId, String name, Long categoryId, BigDecimal calories,
                                       BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat,
                                       BigDecimal fiber) {
        String foodName = name == null ? "" : name.trim();
        if (foodName.isEmpty()) {
            throw new BusinessException("食物名称不能为空");
        }
        Optional<Food> existing = foodRepository.findFirstByCreatedByAndNameAndSource(userId, foodName, SOURCE_USER);
        if (existing.isPresent()) {
            Food food = existing.get();
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
            return foodRepository.save(food);
        }
        return createCustomFood(userId, foodName, categoryId, calories, protein, carbohydrate, fat, fiber);
    }

    /** 创建用户的私有自定义食物 */
    @Transactional
    @CacheEvict(value = "approvedFoods", allEntries = true)
    public Food createCustomFood(Long userId, String name, Long categoryId, BigDecimal calories,
                                 BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat,
                                 BigDecimal fiber) {
        Food food = Food.builder()
                .name(name)
                .categoryId(resolveCategoryId(categoryId))
                .calories(calories)
                .protein(protein)
                .carbohydrate(carbohydrate)
                .fat(fat)
                .fiber(fiber)
                .source(SOURCE_USER)
                .status(STATUS_APPROVED)
                .createdBy(userId)
                .build();
        return foodRepository.save(food);
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

    @Transactional
    @CacheEvict(value = "approvedFoods", allEntries = true)
    public Food updateFood(Long id, AddFoodRequest request) {
        Food food = getFoodById(id);
        food.setName(request.getName());
        food.setCategoryId(request.getCategoryId());
        food.setCalories(request.getCalories());
        food.setProtein(request.getProtein());
        food.setCarbohydrate(request.getCarbohydrate());
        food.setFat(request.getFat());
        food.setFiber(request.getFiber());
        if (request.getImageUrl() != null) {
            food.setImageUrl(request.getImageUrl());
        }
        return foodRepository.save(food);
    }

    @Transactional
    @CacheEvict(value = "approvedFoods", allEntries = true)
    public void updateFoodStatus(Long id, String status) {
        Food food = getFoodById(id);
        food.setStatus(status);
        foodRepository.save(food);
    }

    @Transactional
    @CacheEvict(value = "approvedFoods", allEntries = true)
    public void deleteFood(Long id) {
        foodRepository.deleteById(id);
    }

    public List<Food> getPendingFoods() {
        return foodRepository.findBySourceAndStatus(SOURCE_USER, "pending");
    }

    public Page<Food> getAllFoodsForAdmin(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null && !status.isEmpty()) {
            return foodRepository.findByStatus(status, pageable);
        }
        return foodRepository.findAll(pageable);
    }

    /** 管理端分页查询食物，支持状态与关键字筛选 */
    public Page<Food> getAllFoodsForAdmin(String status, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatus = status != null && !status.isEmpty();
        if (hasKeyword && hasStatus) {
            return foodRepository.searchByKeywordAndStatus(keyword.trim(), status, pageable);
        }
        if (hasKeyword) {
            return foodRepository.searchAllByKeyword(keyword.trim(), pageable);
        }
        if (hasStatus) {
            return foodRepository.findByStatus(status, pageable);
        }
        return foodRepository.findAll(pageable);
    }

    /**
     * 管理端新增系统食物（进入公开食物库，所有用户可见）。
     * 系统食物 source='system'、status='approved'，createdBy 为空。
     */
    @Transactional
    @CacheEvict(value = "approvedFoods", allEntries = true)
    public Food createSystemFood(AddFoodRequest request) {
        if (request.getCategoryId() == null) {
            throw new BusinessException("分类不能为空");
        }
        if (!foodCategoryRepository.existsById(request.getCategoryId())) {
            throw new BusinessException("所选分类不存在");
        }
        Food food = Food.builder()
                .name(request.getName() == null ? null : request.getName().trim())
                .categoryId(request.getCategoryId())
                .calories(request.getCalories())
                .protein(request.getProtein())
                .carbohydrate(request.getCarbohydrate())
                .fat(request.getFat())
                .fiber(request.getFiber())
                .imageUrl(request.getImageUrl())
                .source("system")
                .status(STATUS_APPROVED)
                .build();
        Food saved = foodRepository.save(food);
        log.info("管理端新增系统食物: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }
}
