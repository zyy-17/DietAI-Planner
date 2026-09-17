package com.zyyqq.repository;

import com.zyyqq.entity.NutritionStandard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NutritionStandardRepository extends JpaRepository<NutritionStandard, Long> {

    @Query("SELECT ns FROM NutritionStandard ns WHERE ns.gender = :gender AND :age BETWEEN ns.ageMin AND ns.ageMax")
    Optional<NutritionStandard> findByGenderAndAge(@Param("gender") Integer gender, @Param("age") Integer age);
}