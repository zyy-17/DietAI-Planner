package com.zyyqq.repository;

import com.zyyqq.entity.MealPlanReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MealPlanReviewRepository extends JpaRepository<MealPlanReview, Long> {

    /** 某份食谱的完整审核历史，按时间倒序（最近的在最前） */
    List<MealPlanReview> findByPlanIdOrderByReviewedAtDesc(Long planId);
}
