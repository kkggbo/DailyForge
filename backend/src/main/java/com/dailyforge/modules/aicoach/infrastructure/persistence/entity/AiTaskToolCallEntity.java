package com.dailyforge.modules.aicoach.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ai_task_tool_calls")
public class AiTaskToolCallEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer roundNo;
    private String toolName;
    private String requestSummaryJson;
    private String responseSummaryJson;
    private String status;
    private Integer latencyMs;
    private String errorMessage;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Integer getRoundNo() { return roundNo; }
    public void setRoundNo(Integer roundNo) { this.roundNo = roundNo; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getRequestSummaryJson() { return requestSummaryJson; }
    public void setRequestSummaryJson(String requestSummaryJson) { this.requestSummaryJson = requestSummaryJson; }
    public String getResponseSummaryJson() { return responseSummaryJson; }
    public void setResponseSummaryJson(String responseSummaryJson) { this.responseSummaryJson = responseSummaryJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}