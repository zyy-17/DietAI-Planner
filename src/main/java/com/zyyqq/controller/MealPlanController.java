package com.zyyqq.controller;

import com.zyyqq.dto.request.MealPlanCopyDayRequest;
import com.zyyqq.dto.request.MealPlanGenerateRequest;
import com.zyyqq.dto.request.MealPlanItemAddRequest;
import com.zyyqq.dto.request.MealPlanItemReplaceRequest;
import com.zyyqq.dto.request.MealPlanPublishRequest;
import com.zyyqq.dto.request.MealPlanSaveRequest;
import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.dto.response.FoodAlternativeVO;
import com.zyyqq.dto.response.MealPlanAlternativesVO;
import com.zyyqq.dto.response.MealPlanCurrentVO;
import com.zyyqq.dto.response.MealPlanDayVO;
import com.zyyqq.dto.response.MealPlanDetailVO;
import com.zyyqq.dto.response.MealPlanExecutionVO;
import com.zyyqq.dto.response.MealPlanListVO;
import com.zyyqq.dto.response.MealPlanVO;
import com.zyyqq.entity.MealPlanReview;
import com.zyyqq.service.MealPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 食谱 / 膳食方案接口。
 *
 * <p>这个模块负责三件事：<b>自己写食谱</b>（选食物、填克数，也支持 AI 智能生成初稿）、
 * <b>看今天吃什么</b>（当前执行的食谱 + 今日进度）、<b>食谱广场</b>（发布 / 收藏 / 应用他人食谱）。
 * AI 除生成初稿外，还在单条食物的「替换」里做可选的候选排序。</p>
 *
 * <p>版权约束：从广场「应用」来的食谱属于他人作品，只能替换 / 增删食材，
 * 不能修改基础信息，也不能二次发布。</p>
 */
@RestController
@RequestMapping("/api/meal-plan")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;

    @Value("${app.upload.meal-plan-cover-dir:uploads/meal-plan-covers}")
    private String coverDir;

    @Value("${app.upload.meal-plan-cover-url-prefix:/covers}")
    private String coverUrlPrefix;

    // ── 我的食谱 ───────────────────────────────────────────────────

    /** 新建一份空食谱，落到「我的食谱」里 */
    @PostMapping
    public ApiResponse<MealPlanVO> create(Authentication authentication,
                                         @RequestBody MealPlanSaveRequest request) {
        return ApiResponse.success("已创建", mealPlanService.create(getUserId(authentication), request));
    }

    /**
     * AI 智能生成完整 N 天膳食计划。
     * 创建空食谱 → 调 ai-service 生成 → 解析并写入食物条目 → 返回 planId。
     */
    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(Authentication authentication,
                                                     @RequestBody MealPlanGenerateRequest request) {
        Map<String, Object> result = mealPlanService.generate(getUserId(authentication), request);
        return ApiResponse.success("AI 生成成功", result);
    }

    /** 改食谱基本信息（名称 / 目标 / 周期 / 餐次 / 目标热量） */
    @PutMapping("/{id}")
    public ApiResponse<MealPlanVO> updateBasic(Authentication authentication,
                                              @PathVariable Long id,
                                              @RequestBody MealPlanSaveRequest request) {
        return ApiResponse.success(mealPlanService.updateBasic(getUserId(authentication), id, request));
    }

    /** 当前执行中的食谱 + 今天吃什么 + 今日进度 */
    @GetMapping("/current")
    public ApiResponse<MealPlanCurrentVO> current(Authentication authentication) {
        return ApiResponse.success(mealPlanService.getCurrent(getUserId(authentication)));
    }

    /**
     * 我的食谱列表。
     * status=recipes（不含已归档）/ idle / active / archived，不传则全部。
     */
    @GetMapping
    public ApiResponse<List<MealPlanVO>> list(Authentication authentication,
                                             @RequestParam(required = false) String status) {
        return ApiResponse.success(mealPlanService.list(getUserId(authentication), status));
    }

    /**
     * 我的食谱列表（带状态计数 + 组合筛选）。
     * status：none/pending/approved/rejected/active，不传=全部；
     * days：3/7/14 按周期分组；sort：updated/created。
     */
    @GetMapping("/list")
    public ApiResponse<MealPlanListVO> listWithCounts(Authentication authentication,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Integer days,
                                                      @RequestParam(required = false) String sort) {
        return ApiResponse.success(
                mealPlanService.listWithCounts(getUserId(authentication), status, keyword, days, sort));
    }

    /** 我收藏的食谱 */
    @GetMapping("/favorites")
    public ApiResponse<List<MealPlanVO>> favorites(Authentication authentication) {
        return ApiResponse.success(mealPlanService.myFavorites(getUserId(authentication)));
    }

    /** 食谱详情：逐天明细 */
    @GetMapping("/{id}")
    public ApiResponse<MealPlanDetailVO> detail(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(mealPlanService.detail(getUserId(authentication), id));
    }

    /** 删除食谱 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        mealPlanService.delete(getUserId(authentication), id);
        return ApiResponse.success("已删除", null);
    }

    /** 把食谱设为「正在执行」 */
    @PostMapping("/{id}/apply")
    public ApiResponse<MealPlanVO> apply(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success("已开始执行", mealPlanService.apply(getUserId(authentication), id));
    }

    /** 暂停执行 */
    @PostMapping("/{id}/pause")
    public ApiResponse<MealPlanVO> pause(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success("已暂停", mealPlanService.pause(getUserId(authentication), id));
    }

    /** 恢复执行 */
    @PostMapping("/{id}/resume")
    public ApiResponse<MealPlanVO> resume(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success("已恢复执行", mealPlanService.resume(getUserId(authentication), id));
    }

    /** 提前结束执行（归档） */
    @PostMapping("/{id}/finish")
    public ApiResponse<MealPlanVO> finish(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success("已结束执行", mealPlanService.finish(getUserId(authentication), id));
    }

    /** 执行闭环进度：周期进度 + 每日打卡 + 计划 vs 实际 */
    @GetMapping("/{id}/execution")
    public ApiResponse<MealPlanExecutionVO> execution(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(mealPlanService.getExecutionProgress(getUserId(authentication), id));
    }

    /** 作者查看自己食谱的审核历史 */
    @GetMapping("/{id}/reviews")
    public ApiResponse<List<MealPlanReview>> reviews(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(mealPlanService.reviewHistory(getUserId(authentication), id, false));
    }

    // ── 编辑食谱内容 ───────────────────────────────────────────────

    /** 加一条食物，返回这一天的最新明细 */
    @PostMapping("/{id}/items")
    public ApiResponse<MealPlanDayVO> addItem(Authentication authentication,
                                             @PathVariable Long id,
                                             @RequestBody MealPlanItemAddRequest request) {
        return ApiResponse.success(mealPlanService.addItem(getUserId(authentication), id, request));
    }

    /**
     * 改一条食物：只给 amount 就是改克数；带上 foodId/foodSource（或 foodName）
     * 就是换成另一种食物。
     */
    @PutMapping("/{planId}/item/{itemId}")
    public ApiResponse<MealPlanDayVO> updateItem(Authentication authentication,
                                                @PathVariable Long planId,
                                                @PathVariable Long itemId,
                                                @RequestBody MealPlanItemReplaceRequest request) {
        return ApiResponse.success(
                mealPlanService.updateItem(getUserId(authentication), planId, itemId, request));
    }

    /** 移除一条食物 */
    @DeleteMapping("/{planId}/item/{itemId}")
    public ApiResponse<MealPlanDayVO> removeItem(Authentication authentication,
                                                @PathVariable Long planId,
                                                @PathVariable Long itemId) {
        return ApiResponse.success(mealPlanService.removeItem(getUserId(authentication), planId, itemId));
    }

    /** 复制某一天到另外几天 */
    @PostMapping("/{id}/copy-day")
    public ApiResponse<List<MealPlanDayVO>> copyDay(Authentication authentication,
                                                   @PathVariable Long id,
                                                   @RequestBody MealPlanCopyDayRequest request) {
        return ApiResponse.success(mealPlanService.copyDay(getUserId(authentication), id, request));
    }

    /** 手动替换候选：同类目 + 热量接近，不调 AI */
    @GetMapping("/{planId}/item/{itemId}/candidates")
    public ApiResponse<List<FoodAlternativeVO>> candidates(Authentication authentication,
                                                           @PathVariable Long planId,
                                                           @PathVariable Long itemId) {
        return ApiResponse.success(mealPlanService.itemCandidates(getUserId(authentication), planId, itemId));
    }

    /** 「AI 智能替换」候选（AI 不可用时退化为算法排序，响应里 aiUsed=false） */
    @GetMapping("/{planId}/item/{itemId}/alternatives")
    public ApiResponse<MealPlanAlternativesVO> alternatives(Authentication authentication,
                                                            @PathVariable Long planId,
                                                            @PathVariable Long itemId,
                                                            @RequestParam(required = false) String reason) {
        return ApiResponse.success(
                mealPlanService.alternatives(getUserId(authentication), planId, itemId, reason));
    }

    /** 上传食谱封面 */
    @PostMapping("/{id}/cover")
    public ApiResponse<String> uploadCover(Authentication authentication,
                                          @PathVariable Long id,
                                          @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error("请选择文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.error("只能上传图片文件");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            return ApiResponse.error("图片大小不能超过2MB");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;
        try {
            Path dirPath = Paths.get(coverDir).toAbsolutePath();
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            Files.copy(file.getInputStream(), dirPath.resolve(filename),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String url = coverUrlPrefix + "/" + filename;
            mealPlanService.updateCover(getUserId(authentication), id, url);
            return ApiResponse.success("封面已更新", url);
        } catch (IOException e) {
            return ApiResponse.error("封面上传失败: " + e.getMessage());
        }
    }

    // ── 发布 / 下架 ────────────────────────────────────────────────

    /** 提交发布到食谱广场（进入待审核） */
    @PostMapping("/{id}/publish")
    public ApiResponse<MealPlanVO> publish(Authentication authentication,
                                          @PathVariable Long id,
                                          @RequestBody(required = false) MealPlanPublishRequest request) {
        return ApiResponse.success("已提交审核", mealPlanService.publish(getUserId(authentication), id, request));
    }

    /** 撤回审核 / 下架 */
    @PostMapping("/{id}/unpublish")
    public ApiResponse<MealPlanVO> unpublish(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success("已下架", mealPlanService.unpublish(getUserId(authentication), id));
    }

    // ── 食谱广场 ───────────────────────────────────────────────────

    /** 广场列表：sort=hot 按人气 / new 按最新，可带名称搜索 */
    @GetMapping("/square")
    public ApiResponse<Page<MealPlanVO>> square(Authentication authentication,
                                                @RequestParam(required = false) String sort,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.success(
                mealPlanService.square(getUserId(authentication), sort, keyword, page, size));
    }

    /** 广场食谱详情 */
    @GetMapping("/square/{id}")
    public ApiResponse<MealPlanDetailVO> squareDetail(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(mealPlanService.squareDetail(getUserId(authentication), id));
    }

    /** 收藏 */
    @PostMapping("/square/{id}/favorite")
    public ApiResponse<Boolean> favorite(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(mealPlanService.setFavorite(getUserId(authentication), id, true));
    }

    /** 取消收藏 */
    @DeleteMapping("/square/{id}/favorite")
    public ApiResponse<Boolean> unfavorite(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(mealPlanService.setFavorite(getUserId(authentication), id, false));
    }

    /**
     * 应用他人食谱到「我的食谱」（复制一份到自己账号）。
     *
     * <p>副本会标记为「应用来源食谱」并记录来源追溯信息（原食谱 id + 原作者），
     * 用户只能在其中替换 / 增删食材，不能修改基础信息，也不能发布到广场。</p>
     */
    @PostMapping("/square/{id}/copy")
    public ApiResponse<MealPlanVO> copyToMine(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success("已应用到我的食谱", mealPlanService.copyToMine(getUserId(authentication), id));
    }

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
