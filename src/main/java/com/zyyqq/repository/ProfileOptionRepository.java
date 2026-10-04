package com.zyyqq.repository;

import com.zyyqq.entity.ProfileOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfileOptionRepository extends JpaRepository<ProfileOption, Long> {

    /** 后台：按分组查询全部选项（含停用），排序用 sortOrder */
    List<ProfileOption> findByOptionTypeOrderBySortOrderAscIdAsc(String optionType);

    List<ProfileOption> findByOptionTypeAndStatusOrderBySortOrderAscIdAsc(String optionType, Integer status);

    /** 用户端：一次性取回所有启用选项，按分组归类 */
    List<ProfileOption> findByStatusOrderByOptionTypeAscSortOrderAscIdAsc(Integer status);

    /** 分组内 code 唯一性校验（新增/改 code 时用） */
    boolean existsByOptionTypeAndOptionCodeAndIdNot(String optionType, String optionCode, Long id);

    boolean existsByOptionTypeAndOptionCode(String optionType, String optionCode);

    /** 判断某个分组下是否已无任何选项——用于种子数据"仅在空表时插入" */
    boolean existsByOptionType(String optionType);
}
