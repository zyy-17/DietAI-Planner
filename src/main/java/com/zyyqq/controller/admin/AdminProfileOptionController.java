package com.zyyqq.controller.admin;

import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.entity.ProfileOption;
import com.zyyqq.entity.ProfileOptionType;
import com.zyyqq.service.ProfileOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端「档案选项」管理接口。
 *
 * <p>对应客户端个人中心的饮食目标 / 活动水平 / 饮食偏好 / 忌口食物 / 慢性疾病
 * 这些下拉选项，管理员可在此增删改查。</p>
 */
@RestController
@RequestMapping("/api/admin/profile-options")
@RequiredArgsConstructor
public class AdminProfileOptionController {

    private final ProfileOptionService profileOptionService;

    /** 全部可选分组（含中文名），供后台渲染页签 */
    @GetMapping("/types")
    public ApiResponse<List<Map<String, String>>> types() {
        List<Map<String, String>> list = new java.util.ArrayList<>();
        for (String type : ProfileOptionType.ALL) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("value", type);
            item.put("label", ProfileOptionType.label(type));
            list.add(item);
        }
        return ApiResponse.success(list);
    }

    /** 某分组的全部选项（含停用项） */
    @GetMapping
    public ApiResponse<List<ProfileOption>> list(@RequestParam String type) {
        return ApiResponse.success(profileOptionService.listForAdmin(type));
    }

    /** 新增选项 */
    @PostMapping
    public ApiResponse<ProfileOption> create(@RequestBody ProfileOption option) {
        return ApiResponse.success("新增成功", profileOptionService.create(option));
    }

    /** 更新选项 */
    @PutMapping("/{id}")
    public ApiResponse<ProfileOption> update(@PathVariable Long id, @RequestBody ProfileOption option) {
        return ApiResponse.success("更新成功", profileOptionService.update(id, option));
    }

    /** 删除选项（已有用户使用时会被拒绝，应改为停用） */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        profileOptionService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
