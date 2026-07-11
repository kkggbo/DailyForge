package com.dailyforge.modules.auth.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_invite_code_usages")
public class UserInviteCodeUsageEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long inviteCodeId;
    private LocalDateTime usedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getInviteCodeId() {
        return inviteCodeId;
    }

    public void setInviteCodeId(Long inviteCodeId) {
        this.inviteCodeId = inviteCodeId;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
}
