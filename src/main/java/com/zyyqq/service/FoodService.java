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

    public Food getFoodById(Long id) {
        return foodRepository.findById(id)
                .orElseThrow(() -> new BusinessException("食物不存在"));
    }

    public List<Food> searchFoods(String keyword) {
        return foodRepository.searchApproved(keyword);
    }

    public List<Food> getAllApprovedFoods() {
        return foodRepository.findByStatus("approved");
    }

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
                .status("pending")
                .createdBy(userId)
                .build();
        return foodRepository.save(food);
    }

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

    @Transactional
    public void updateFoodStatus(Long id, String status) {
        Food food = getFoodById(id);
        food.setStatus(status);
        foodRepository.save(food);
    }

    @Transactional
    public void deleteFood(Long id) {
        foodRepository.deleteById(id);
    }

    public List<Food> getPendingFoods() {
        return foodRepository.findBySourceAndStatus("user", "pending");
    }

    public Page<Food> getAllFoodsForAdmin(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null && !status.isEmpty()) {
            return foodRepository.findByStatus(status, pageable);
        }
        return foodRepository.findAll(pageable);
    }
}