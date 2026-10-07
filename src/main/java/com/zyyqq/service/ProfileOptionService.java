package com.zyyqq.service;

import com.zyyqq.entity.ProfileOption;
import com.zyyqq.entity.ProfileOptionType;
import com.zyyqq.entity.User;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.ProfileOptionRepository;
import com.zyyqq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户档案选项（饮食目标 / 活动水平 / 饮食偏好 / 忌口 / 疾病）的管理。
 */
@Service
@RequiredArgsConstructor
public class ProfileOptionService {

    private static final long DICT_CACHE_MS = 60_000L;

    private final ProfileOptionRepository repository;
    private final UserRepository userRepository;

    /** code→label 字典缓存，写操作时整体清空 */
    private final Map<String, Map<String, String>> dictCache = new ConcurrentHashMap<>();
    private volatile long dictCacheTime = 0L;

    // ── 后台管理端 ────────────────────────────────────────────────

    /** 某分组的全部选项（含停用），后台表格用 */
    public List<ProfileOption> listForAdmin(String optionType) {
        checkType(optionType);
        return repository.findByOptionTypeOrderBySortOrderAscIdAsc(optionType);
    }

    @Transactional
    public ProfileOption create(ProfileOption option) {
        normalize(option);
        option.setId(null);
        checkType(option.getOptionType());
        if (repository.existsByOptionTypeAndOptionCode(option.getOptionType(), option.getOptionCode())) {
            throw new BusinessException("该分组下已存在选项值「" + option.getOptionCode() + "」");
        }
        if (option.getStatus() == null) option.setStatus(1);
        ProfileOption saved = repository.save(option);
        evictCache();
        return saved;
    }

    @Transactional
    public ProfileOption update(Long id, ProfileOption form) {
        ProfileOption existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException("选项不存在"));

        // 分组不允许改：改了会让老用户存的 code 落到另一个语义下
        if (form.getOptionType() != null && !form.getOptionType().equals(existing.getOptionType())) {
            throw new BusinessException("选项所属分组不可修改");
        }
        normalize(form);

        if (!form.getOptionCode().equals(existing.getOptionCode())
                && repository.existsByOptionTypeAndOptionCodeAndIdNot(
                        existing.getOptionType(), form.getOptionCode(), id)) {
            throw new BusinessException("该分组下已存在选项值「" + form.getOptionCode() + "」");
        }

        existing.setOptionCode(form.getOptionCode());
        existing.setOptionLabel(form.getOptionLabel());
        if (form.getSortOrder() != null) existing.setSortOrder(form.getSortOrder());
        if (form.getStatus() != null) existing.setStatus(form.getStatus());
        existing.setRemark(form.getRemark());
        ProfileOption saved = repository.save(existing);
        evictCache();
        return saved;
    }

    /**
     * 删除选项。
     *
     * <p>若已有用户把该值存进档案，则<b>不允许物理删除</b>——那会让这些用户的档案
     * 出现无法识别的孤儿值，界面上变成空白。此时引导管理员改用"停用"：
     * 停用后新用户选不到，老用户仍能正常显示。</p>
     */
    @Transactional
    public void delete(Long id) {
        ProfileOption existing = repository.findById(id)
                .orElseThrow(() -> new BusinessException("选项不存在"));

        int usedBy = countUsersUsing(existing);
        if (usedBy > 0) {
            throw new BusinessException(String.format(
                    "已有 %d 位用户使用「%s」，不能删除。请改为「停用」——停用后新用户不再显示，已有用户不受影响。",
                    usedBy, existing.getOptionLabel()));
        }
        repository.deleteById(id);
        evictCache();
    }

    /** 统计有多少用户的档案里用到了这个选项值 */
    private int countUsersUsing(ProfileOption option) {
        String code = option.getOptionCode();
        List<User> candidates = switch (option.getOptionType()) {
            case ProfileOptionType.DIET_GOAL -> userRepository.findByDietGoal(code);
            case ProfileOptionType.ACTIVITY_LEVEL -> {
                Integer v = parseActivityValue(code);
                yield v == null ? List.of() : userRepository.findByActivityLevel(v);
            }
            case ProfileOptionType.DIET_PREFERENCE -> userRepository.findByDietPreferenceIsNotNull();
            case ProfileOptionType.ALLERGY -> userRepository.findByAllergyNoteIsNotNull();
            case ProfileOptionType.DISEASE -> userRepository.findByDiseaseIsNotNull();
            default -> List.of();
        };
        if (candidates.isEmpty()) return 0;

        if (ProfileOptionType.DIET_GOAL.equals(option.getOptionType())
                || ProfileOptionType.ACTIVITY_LEVEL.equals(option.getOptionType())) {
            // 单选字段已由数据库精确匹配
            return candidates.size();
        }
        // 多选字段以「、」分隔，须按分隔符精确比对，避免"花生"命中"花生酱"
        int count = 0;
        for (User u : candidates) {
            if (containsCode(getFieldValue(u, option.getOptionType()), code)) count++;
        }
        return count;
    }

    private String getFieldValue(User user, String type) {
        return switch (type) {
            case ProfileOptionType.DIET_PREFERENCE -> user.getDietPreference();
            case ProfileOptionType.ALLERGY -> user.getAllergyNote();
            case ProfileOptionType.DISEASE -> user.getDisease();
            default -> null;
        };
    }

    /** 判断以「、」分隔的多选字符串里是否含有某个 code（精确到整段） */
    private boolean containsCode(String joined, String code) {
        if (joined == null || joined.isBlank()) return false;
        for (String part : joined.split("[、,，]")) {
            if (code.equals(part.trim())) return true;
        }
        return false;
    }

    /** 活动水平的 code 是数字字符串，非法值返回 null（不计入引用统计） */
    private Integer parseActivityValue(String code) {
        try {
            return Integer.valueOf(code);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── 用户端 ────────────────────────────────────────────────────

    /**
     * 返回所有启用选项，按分组归类。
     * key 为 optionType，value 为按 sortOrder 排好序的选项列表。
     */
    public Map<String, List<ProfileOption>> listEnabledGrouped() {
        Map<String, List<ProfileOption>> grouped = new LinkedHashMap<>();
        for (String type : ProfileOptionType.ALL) {
            grouped.put(type, new ArrayList<>());
        }
        for (ProfileOption option : repository.findByStatusOrderByOptionTypeAscSortOrderAscIdAsc(1)) {
            grouped.computeIfAbsent(option.getOptionType(), k -> new ArrayList<>()).add(option);
        }
        return grouped;
    }

    /**
     * 把用户存的 code 翻译成 label，用于 AI 上下文等场景。
     * 查不到时原样返回 code——即使选项被删了也不会丢信息。
     */
    public String labelOf(String optionType, String code) {
        if (code == null || code.isBlank()) return null;
        String trimmed = code.trim();
        return codeToLabel(optionType).getOrDefault(trimmed, trimmed);
    }

    /** 批量翻译（多选场景，code 以「、」分隔），查不到的项保留原文 */
    public String labelsOf(String optionType, String codes) {
        if (codes == null || codes.isBlank()) return null;
        Map<String, String> dict = codeToLabel(optionType);
        StringBuilder sb = new StringBuilder();
        for (String raw : codes.split("[、,，]")) {
            String code = raw.trim();
            if (code.isEmpty()) continue;
            if (sb.length() > 0) sb.append("、");
            sb.append(dict.getOrDefault(code, code));
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    // ── 内部工具 ──────────────────────────────────────────────────

    /**
     * code → label 字典，带 60 秒软缓存。
     * 一次 AI 请求里会多次翻译同一分组，逐行查库没必要；
     * 缓存只是减少查询，后台改动选项最长 1 分钟内生效。
     */
    private Map<String, String> codeToLabel(String optionType) {
        long now = System.currentTimeMillis();
        Map<String, String> cached = dictCache.get(optionType);
        if (cached != null && now - dictCacheTime < DICT_CACHE_MS) {
            return cached;
        }
        Map<String, String> map = new HashMap<>();
        for (ProfileOption o : repository.findByOptionTypeOrderBySortOrderAscIdAsc(optionType)) {
            map.put(o.getOptionCode(), o.getOptionLabel());
        }
        dictCache.put(optionType, map);
        dictCacheTime = now;
        return map;
    }

    /** 任何写操作后清空缓存 */
    private void evictCache() {
        dictCache.clear();
    }

    private void normalize(ProfileOption option) {
        if (option.getOptionCode() != null) option.setOptionCode(option.getOptionCode().trim());
        if (option.getOptionLabel() != null) option.setOptionLabel(option.getOptionLabel().trim());
        if (option.getSortOrder() == null) option.setSortOrder(0);
    }

    private void checkType(String type) {
        for (String t : ProfileOptionType.ALL) {
            if (t.equals(type)) return;
        }
        throw new BusinessException("未知的选项分组：" + type);
    }
}
