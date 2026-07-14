package com.dailyforge.modules.plan.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateDayEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CycleTemplateDayMapper extends BaseMapper<CycleTemplateDayEntity> {

    /**
     * Return all template days under one version sorted by day index.
     */
    @Select("SELECT * FROM cycle_template_days WHERE template_version_id = #{versionId} ORDER BY day_index ASC")
    List<CycleTemplateDayEntity> selectByVersionId(Long versionId);

    @Delete("DELETE FROM cycle_template_days WHERE template_version_id = #{versionId}")
    int deleteByVersionId(Long versionId);
}
