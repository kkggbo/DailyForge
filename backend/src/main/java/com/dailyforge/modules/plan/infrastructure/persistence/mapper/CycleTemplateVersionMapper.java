package com.dailyforge.modules.plan.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateVersionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CycleTemplateVersionMapper extends BaseMapper<CycleTemplateVersionEntity> {

    @Select("SELECT COALESCE(MAX(version_no), 0) FROM cycle_template_versions WHERE template_id = #{templateId}")
    Integer selectMaxVersionNo(Long templateId);
}
