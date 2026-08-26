package com.dailyforge.modules.aicoach.infrastructure.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dailyforge.ai")
public class AiCoachProperties {

    private boolean enabled = true;
    private String provider = "deepseek";
    private String model = "deepseek-chat";
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey = "";
    private Duration timeout = Duration.ofSeconds(120);
    private int maxToolRounds = 50;
    private int maxRepairAttempts = 2;
    private String templateGenerationPromptVersion = "template_generation_v1";
    private String cycleSummaryPromptVersion = "cycle_summary_v1";
    private String nextCycleGenerationPromptVersion = "next_cycle_generation_v1";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public int getMaxToolRounds() { return maxToolRounds; }
    public void setMaxToolRounds(int maxToolRounds) { this.maxToolRounds = maxToolRounds; }
    public int getMaxRepairAttempts() { return maxRepairAttempts; }
    public void setMaxRepairAttempts(int maxRepairAttempts) { this.maxRepairAttempts = maxRepairAttempts; }
    public String getTemplateGenerationPromptVersion() { return templateGenerationPromptVersion; }
    public void setTemplateGenerationPromptVersion(String templateGenerationPromptVersion) { this.templateGenerationPromptVersion = templateGenerationPromptVersion; }
    public String getCycleSummaryPromptVersion() { return cycleSummaryPromptVersion; }
    public void setCycleSummaryPromptVersion(String cycleSummaryPromptVersion) { this.cycleSummaryPromptVersion = cycleSummaryPromptVersion; }
    public String getNextCycleGenerationPromptVersion() { return nextCycleGenerationPromptVersion; }
    public void setNextCycleGenerationPromptVersion(String nextCycleGenerationPromptVersion) { this.nextCycleGenerationPromptVersion = nextCycleGenerationPromptVersion; }
}
