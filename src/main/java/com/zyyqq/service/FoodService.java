package com.zyyqq.service;

import com.zyyqq.dto.request.AddFoodRequest;
import com.zyyqq.entity.Food;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.FoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodRepository foodRepository;

    /** 分页查询已审核食物，支持按分类和关键词筛选 */
    public Page<Food> getFoods(Long categoryId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (keyword != null && !keyword.isEmpty()) {
            return foodRepository.searchByKeywordAndStatus(keyword, "approved", pageable);
        }
        if (categoryId != null) {
            return foodRepository.findByCategoryIdAndStatus(categoryId, "approved", pageable);
        }
        return foodRepository.findByStatus("approved", pageable);
    }

    /** 根据ID获取食物，不存在则抛异常 */
    public Food getFoodById(Long id) {
        return foodRepository.findById(id)
                .orElseThrow(() -> new BusinessException("食物不存在"));
    }

    /** 搜索已审核食物（不分页，用于快速添加） */
    public List<Food> searchFoods(String keyword) {
        return foodRepository.searchApproved(keyword);
    }

    /** 获取所有已审核食物列表 */
    public List<Food> getAllApprovedFoods() {
        return foodRepository.findByStatus("approved");
    }

    /** 用户添加自定义食物，直接可用 */
    @Transactional
    public Food addFoodByUser(AddFoodRequest request, Long userId) {
        Food food = Food.builder()
                .name(request.getName())
                .categoryId(request.getCategoryId())
                .calories(request.getCalories())
                .protein(request.getProtein())
                .carbohydrate(request.getCarbohydrate())
                .fat(request.getFat())
                .fiber(request.getFiber())
                .imageUrl(request.getImageUrl())
                .source("user")
                .status("approved")
                .createdBy(userId)
                .build();
        return foodRepository.save(food);
    }

    /** 更新食物信息 */
    @Transactional
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

    /** 更新食物审核状态 */
    @Transactional
    public void updateFoodStatus(Long id, String status) {
        Food food = getFoodById(id);
        food.setStatus(status);
        foodRepository.save(food);
    }

    /** 删除食物 */
    @Transactional
    public void deleteFood(Long id) {
        foodRepository.deleteById(id);
    }

    /** 获取用户提交的待审核食物 */
    public List<Food> getPendingFoods() {
        return foodRepository.findBySourceAndStatus("user", "pending");
    }

    /** 管理端分页查询所有食物，支持按状态筛选 */
    public Page<Food> getAllFoodsForAdmin(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null && !status.isEmpty()) {
            return foodRepository.findByStatus(status, pageable);
        }
        return foodRepository.findAll(pageable);
    }
}