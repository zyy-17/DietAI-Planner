package com.zyyqq.service;

import com.zyyqq.dto.request.AddFoodRequest;
import com.zyyqq.dto.response.FoodOptionVO;
import com.zyyqq.entity.Food;
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

/**
 * 公共食物库（food 表）业务。
 * <p>
 * 食物库只存放系统食物（source='system'），由管理员维护；
 * 用户自定义食物存放在 user_custom_food 表，见 {@link UserCustomFoodService}，
 * 两者物理隔离，用户自建的食物不会进入食物库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodService {

    /** 用户自定义食物来源标记（历史数据兼容：食物库查询会排除该来源） */
    private static final String SOURCE_USER = "user";
    /** 系统食物来源标记（管理端只管理这一类） */
    private static final String SOURCE_SYSTEM = "system";
    /** 审核通过状态 */
    private static final String STATUS_APPROVED = "approved";

    private final FoodRepository foodRepository;
    private final FoodCategoryRepository foodCategoryRepository;
    private final UserCustomFoodService userCustomFoodService;

    /** 公共食物库分页查询（仅系统食物） */
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

    public List<Food> searchFoods(String keyword) {
        return foodRepository.searchPublicApproved(keyword);
    }

    /** 公开食物库（仅系统食物），供内部名称匹配等场景使用 */
    @Cacheable(value = "approvedFoods", key = "'public'")
    public List<Food> getAllApprovedFoods() {
        return foodRepository.findPublicFoodList(STATUS_APPROVED, SOURCE_USER);
    }

    /**
     * 当前用户可选的食物：公共食物库 + 本人自定义食物。
     * 自定义食物来自 user_custom_food 表，仅本人可见。
     */
    public List<FoodOptionVO> getAvailableFoodOptions(Long userId) {
        List<FoodOptionVO> options = new ArrayList<>();
        for (Food food : getAllApprovedFoods()) {
            options.add(FoodOptionVO.fromFood(food));
        }
        options.addAll(userCustomFoodService.listMineAsOptions(userId));
        return options;
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
        applyUnit(food, request.getUnitName(), request.getUnitWeight());
        return foodRepository.save(food);
    }

    /** 写入计量单位信息：单位名为空时一并清掉参考克重，避免出现半截数据 */
    private void applyUnit(Food food, String unitName, BigDecimal unitWeight) {
        String name = unitName == null ? null : unitName.trim();
        if (name == null || name.isEmpty()) {
            food.setUnitName(null);
            food.setUnitWeight(null);
            return;
        }
        food.setUnitName(name);
        food.setUnitWeight(unitWeight != null && unitWeight.compareTo(BigDecimal.ZERO) > 0 ? unitWeight : null);
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

    /**
     * 管理端分页查询食物库，支持名称关键字筛选。
     * 只返回系统食物（source='system'），用户自定义食物不在管理端展示。
     */
    public Page<Food> getAllFoodsForAdmin(String status, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatus = status != null && !status.isEmpty();
        if (hasKeyword && hasStatus) {
            return foodRepository.searchAdminFoodsByStatus(SOURCE_SYSTEM, status, keyword.trim(), pageable);
        }
        if (hasKeyword) {
            return foodRepository.searchAdminFoods(SOURCE_SYSTEM, keyword.trim(), pageable);
        }
        if (hasStatus) {
            return foodRepository.findAdminFoodsByStatus(SOURCE_SYSTEM, status, pageable);
        }
        return foodRepository.findAdminFoods(SOURCE_SYSTEM, pageable);
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
                .source(SOURCE_SYSTEM)
                .status(STATUS_APPROVED)
                .build();
        applyUnit(food, request.getUnitName(), request.getUnitWeight());
        Food saved = foodRepository.save(food);
        log.info("管理端新增系统食物: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }
}
