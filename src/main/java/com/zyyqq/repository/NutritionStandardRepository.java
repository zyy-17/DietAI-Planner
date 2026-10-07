package com.zyyqq.repository;

import com.zyyqq.entity.NutritionStandard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NutritionStandardRepository extends JpaRepository<NutritionStandard, Long> {

    @Query("SELECT ns FROM NutritionStandard ns WHERE ns.gender = :gender AND :age BETWEEN ns.ageMin AND ns.ageMax")
    Optional<NutritionStandard> findByGenderAndAge(@Param("gender") Integer gender, @Param("age") Integer age);

    /** 管理端：按性别、年龄段排序查询全部营养标准 */
    List<NutritionStandard> findAllByOrderByGenderAscAgeMinAsc();
}