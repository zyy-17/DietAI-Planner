package com.zyyqq.controller;

import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.service.ProfileOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 客户端拉取个人中心的可选项（饮食目标、活动水平、饮食偏好、忌口、慢性疾病）。
 *
 * <p>前端不再硬编码这些下拉项，全部由此接口下发，管理员在后台改动后刷新即可生效。</p>
 */
@RestController
@RequestMapping("/api/user/options")
@RequiredArgsConstructor
public class UserOptionController {

    private final ProfileOptionService profileOptionService;

    /**
     * 返回所有启用选项，按分组归类。
     * 形如 { "diet_goal": [{optionCode, optionLabel, ...}, ...], ... }
     */
    @GetMapping
    public ApiResponse<Map<String, List<Map<String, Object>>>> options() {
        Map<String, List<Map<String, Object>>> result = new java.util.LinkedHashMap<>();
        profileOptionService.listEnabledGrouped().forEach((type, options) -> {
            List<Map<String, Object>> items = new java.util.ArrayList<>(options.size());
            for (var o : options) {
                Map<String, Object> item = new java.util.LinkedHashMap<>();
                item.put("code", o.getOptionCode());
                item.put("label", o.getOptionLabel());
                items.add(item);
            }
            result.put(type, items);
        });
        return ApiResponse.success(result);
    }
}
