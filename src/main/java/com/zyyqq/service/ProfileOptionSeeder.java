package com.zyyqq.service;

import com.zyyqq.entity.ProfileOption;
import com.zyyqq.entity.ProfileOptionType;
import com.zyyqq.repository.ProfileOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户档案选项的默认数据初始化。
 *
 * <p>把原先硬编码在 Profile.vue 里的选项搬到数据库，让管理员可在后台增删改查。</p>
 *
 * <p><b>幂等策略</b>：只在某个分组<b>一条都没有</b>时才插入该分组的默认值。
 * 这样管理员删光某个分组后重启服务，选项不会被"复活"；
 * 只删了一部分则完全不动，不会覆盖已有配置。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileOptionSeeder implements ApplicationRunner {

    private final ProfileOptionRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        int inserted = 0;
        inserted += seedIfEmpty(ProfileOptionType.DIET_GOAL, List.of(
                opt("lose", "减脂", 1, "低热量高蛋白，缺口 15%~20%"),
                opt("maintain", "维持", 2, "保持当前体重，热量持平"),
                opt("gain", "增肌", 3, "热量盈余 10%~15%，配合力量训练")
        ));
        inserted += seedIfEmpty(ProfileOptionType.ACTIVITY_LEVEL, List.of(
                opt("1", "久坐（几乎不运动）", 1, "久坐办公，基本不锻炼"),
                opt("2", "轻度活动（每周1-3次）", 2, "每周运动 1~3 次"),
                opt("3", "中度活动（每周3-5次）", 3, "每周运动 3~5 次"),
                opt("4", "高度活动（每周6-7次）", 4, "每周运动 6~7 次"),
                opt("5", "极高活动（体力劳动）", 5, "重体力劳动或运动员")
        ));
        inserted += seedIfEmpty(ProfileOptionType.DIET_PREFERENCE, List.of(
                opt("清淡", "清淡", 1, null),
                opt("中式", "中式", 2, null),
                opt("西式", "西式", 3, null),
                opt("素食", "素食", 4, null),
                opt("低糖", "低糖", 5, null),
                opt("低脂", "低脂", 6, null),
                opt("高蛋白", "高蛋白", 7, null),
                opt("无辣", "无辣", 8, null),
                opt("地中海", "地中海饮食", 9, null),
                opt("生酮", "生酮饮食", 10, null)
        ));
        inserted += seedIfEmpty(ProfileOptionType.ALLERGY, List.of(
                opt("海鲜", "海鲜", 1, "虾蟹贝类"),
                opt("牛奶", "牛奶", 2, "乳制品"),
                opt("鸡蛋", "鸡蛋", 3, null),
                opt("花生", "花生", 4, null),
                opt("大豆", "大豆", 5, null),
                opt("麸质", "麸质", 6, "小麦、大麦等"),
                opt("坚果", "坚果", 7, null)
        ));
        inserted += seedIfEmpty(ProfileOptionType.DISEASE, List.of(
                opt("糖尿病", "糖尿病", 1, "需控糖"),
                opt("高血压", "高血压", 2, "需控盐"),
                opt("高血脂", "高血脂", 3, "需控脂"),
                opt("痛风", "痛风", 4, "需控嘌呤"),
                opt("脂肪肝", "脂肪肝", 5, "需控总热量")
        ));

        if (inserted > 0) {
            log.info("用户档案选项初始化完成，新增 {} 条默认选项", inserted);
        } else {
            log.info("用户档案选项已存在，跳过初始化");
        }
    }

    /** 仅当该分组无任何选项时插入，返回实际插入条数 */
    private int seedIfEmpty(String type, List<ProfileOption> defaults) {
        if (repository.existsByOptionType(type)) {
            return 0;
        }
        // 逐条补上分组字段后落库（builder 里不便引用 type）
        List<ProfileOption> toSave = new ArrayList<>(defaults.size());
        for (ProfileOption o : defaults) {
            o.setOptionType(type);
            toSave.add(o);
        }
        repository.saveAll(toSave);
        return toSave.size();
    }

    private static ProfileOption opt(String code, String label, int sort, String remark) {
        return ProfileOption.builder()
                .optionCode(code)
                .optionLabel(label)
                .sortOrder(sort)
                .status(1)
                .remark(remark)
                .build();
    }
}
