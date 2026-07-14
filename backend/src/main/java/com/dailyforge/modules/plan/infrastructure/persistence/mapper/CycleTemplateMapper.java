package com.dailyforge.modules.plan.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CycleTemplateMapper extends BaseMapper<CycleTemplateEntity> {

    @Select("SELECT * FROM cycle_templates WHERE id = #{templateId} AND user_id = #{userId} LIMIT 1")
    CycleTemplateEntity selectByIdAndUserId(@Param("templateId") Long templateId, @Param("userId") Long userId);

    @Select("SELECT * FROM cycle_templates WHERE user_id = #{userId} AND status = 'active' LIMIT 1")
    CycleTemplateEntity selectActiveByUserId(Long userId);

    @Select("SELECT * FROM cycle_templates WHERE id = #{templateId} AND user_id = #{userId} LIMIT 1 FOR UPDATE")
    CycleTemplateEntity selectByIdAndUserIdForUpdate(
            @Param("templateId") Long templateId,
            @Param("userId") Long userId);
}
