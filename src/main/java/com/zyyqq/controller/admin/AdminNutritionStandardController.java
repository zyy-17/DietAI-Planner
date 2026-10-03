package com.zyyqq.controller.admin;

import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.entity.NutritionStandard;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.NutritionStandardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端营养标准管理接口（对应客户端"营养分析"模块所依据的参考标准）。
 */
@RestController
@RequestMapping("/api/admin/nutrition-standards")
@RequiredArgsConstructor
public class AdminNutritionStandardController {

    private final NutritionStandardRepository nutritionStandardRepository;

    /** 查询全部营养标准（按性别、年龄段排序） */
    @GetMapping
    public ApiResponse<List<NutritionStandard>> list() {
        return ApiResponse.success(nutritionStandardRepository.findAllByOrderByGenderAscAgeMinAsc());
    }

    /** 新增一条营养标准 */
    @PostMapping
    public ApiResponse<NutritionStandard> create(@RequestBody NutritionStandard standard) {
        validate(standard);
        standard.setId(null);
        return ApiResponse.success("新增成功", nutritionStandardRepository.save(standard));
    }

    /** 更新营养标准 */
    @PutMapping("/{id}")
    public ApiResponse<NutritionStandard> update(@PathVariable Long id, @RequestBody NutritionStandard standard) {
        validate(standard);
        NutritionStandard existing = nutritionStandardRepository.findById(id)
                .orElseThrow(() -> new BusinessException("营养标准不存在"));
        existing.setGender(standard.getGender());
        existing.setAgeMin(standard.getAgeMin());
        existing.setAgeMax(standard.getAgeMax());
        existing.setCaloriesKcal(standard.getCaloriesKcal());
        existing.setProteinG(standard.getProteinG());
        existing.setCarbG(standard.getCarbG());
        existing.setFatG(standard.getFatG());
        return ApiResponse.success("更新成功", nutritionStandardRepository.save(existing));
    }

    /** 删除营养标准 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        if (!nutritionStandardRepository.existsById(id)) {
            throw new BusinessException("营养标准不存在");
        }
        nutritionStandardRepository.deleteById(id);
        return ApiResponse.success("删除成功", null);
    }

    /** 基础字段校验 */
    private void validate(NutritionStandard standard) {
        if (standard.getGender() == null || standard.getAgeMin() == null
                || standard.getAgeMax() == null || standard.getCaloriesKcal() == null) {
            throw new BusinessException("性别、年龄段和推荐热量不能为空");
        }
        if (standard.getAgeMin() > standard.getAgeMax()) {
            throw new BusinessException("起始年龄不能大于结束年龄");
        }
    }
}
