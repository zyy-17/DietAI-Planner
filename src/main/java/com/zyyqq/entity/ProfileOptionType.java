package com.zyyqq.entity;

/**
 * 用户档案选项的分组类型。
 *
 * <p>用常量集中管理，替代散落在前端各处的魔法字符串。
 * 新增一种选项时，这里加一个枚举值 + 在 {@link ProfileOptionSeeder} 补默认数据即可，
 * 后台页面会自动多出一个分组页签。</p>
 */
public final class ProfileOptionType {

    private ProfileOptionType() {}

    /** 饮食目标：减脂 / 维持 / 增肌（存 user.diet_goal，单选） */
    public static final String DIET_GOAL = "diet_goal";

    /** 活动水平：久坐 / 轻度 / 中度…（存 user.activity_level，整数单选） */
    public static final String ACTIVITY_LEVEL = "activity_level";

    /** 饮食偏好：清淡 / 中式 / 低糖…（多选，存 user.diet_preference） */
    public static final String DIET_PREFERENCE = "diet_preference";

    /** 忌口食物：海鲜 / 牛奶 / 花生…（多选） */
    public static final String ALLERGY = "allergy";

    /** 慢性疾病：糖尿病 / 高血压…（多选） */
    public static final String DISEASE = "disease";

    /** 全部类型，供后台页签渲染与数据初始化遍历 */
    public static final String[] ALL = {
            DIET_GOAL, ACTIVITY_LEVEL, DIET_PREFERENCE, ALLERGY, DISEASE
    };

    /** 类型的中文名，后台页签与接口返回都用它 */
    public static String label(String type) {
        if (type == null) return "未知";
        return switch (type) {
            case DIET_GOAL -> "饮食目标";
            case ACTIVITY_LEVEL -> "活动水平";
            case DIET_PREFERENCE -> "饮食偏好";
            case ALLERGY -> "忌口食物";
            case DISEASE -> "慢性疾病";
            default -> type;
        };
    }
}
