package com.zyyqq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户档案可选项（字典表）。
 *
 * <p>客户端「个人中心」里的饮食目标、活动水平、饮食偏好、忌口食物、慢性疾病
 * 全部是下拉/多选，管理员可在后台增删改查。原先这些选项硬编码在前端，
 * 改一次要重新发版，且各页面（Profile / AiChat / AiSuggest）各写一份容易不一致。</p>
 *
 * <p>设计要点：<b>code 是存进 user 表的真实值，label 是给用户看的文案</b>。
 * code 一旦有用户数据就不该再改（否则老数据变成非法值），label 可以随时改。
 * 删除用 status=0 停用而非物理删除，同样是为了不破坏已有用户数据。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "profile_option")
public class ProfileOption {

    /** 选项所属分组，取值见 {@link ProfileOptionType} */
    @Column(name = "option_type", nullable = false, length = 30)
    private String optionType;

    /** 存入 user 表的值，如 lose / 1 / 清淡。分组内唯一 */
    @Column(name = "option_code", nullable = false, length = 50)
    private String optionCode;

    /** 界面展示文案，如 减脂 / 久坐（几乎不运动） */
    @Column(name = "option_label", nullable = false, length = 100)
    private String optionLabel;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /** 1=启用 0=停用（停用后用户端不再展示，历史用户数据不受影响） */
    @Column(nullable = false)
    @Builder.Default
    private Integer status = 1;

    /** 备注，仅后台可见，用于提醒管理员这个选项的用途 */
    @Column(length = 255)
    private String remark;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
