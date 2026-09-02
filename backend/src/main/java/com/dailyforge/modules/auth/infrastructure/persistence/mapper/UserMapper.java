package com.dailyforge.modules.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("SELECT * FROM users WHERE email = #{email} LIMIT 1")
    UserEntity selectByEmail(String email);

    @Select("SELECT * FROM users WHERE user_name = #{userName} LIMIT 1")
    UserEntity selectByUserName(String userName);

    @Select("""
            <script>
            SELECT * FROM users
            WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            </script>
            """)
    List<UserEntity> selectByIds(@Param("ids") List<Long> ids);
}
