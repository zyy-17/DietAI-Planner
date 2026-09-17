package com.zyyqq.service;

import com.zyyqq.entity.FoodCategory;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.FoodCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodCategoryService {

    private final FoodCategoryRepository foodCategoryRepository;

    public List<FoodCategory> getAllCategories() {
        return foodCategoryRepository.findAllEnabled();
    }

    public List<FoodCategory> getSubCategories(Long parentId) {
        return foodCategoryRepository.findByParentIdAndStatusOrderBySortOrderAsc(parentId, 1);
    }

    public FoodCategory getCategoryById(Long id) {
        return foodCategoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("分类不存在"));
    }

    @Transactional
    public FoodCategory createCategory(FoodCategory category) {
        return foodCategoryRepository.save(category);
    }

    @Transactional
    public FoodCategory updateCategory(Long id, FoodCategory category) {
        FoodCategory existing = getCategoryById(id);
        existing.setName(category.getName());
        existing.setParentId(category.getParentId());
        existing.setSortOrder(category.getSortOrder());
        existing.setIcon(category.getIcon());
        existing.setStatus(category.getStatus());
        return foodCategoryRepository.save(existing);
    }

    @Transactional
    public void deleteCategory(Long id) {
        foodCategoryRepository.deleteById(id);
    }
}