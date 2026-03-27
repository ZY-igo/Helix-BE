package com.sipc115.helix.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class BotProperties {

    private final Zhipu zhipu = new Zhipu();
    private final Feishu feishu = new Feishu();
    private final Report report = new Report();
    private List<String> sourceWhitelist = new ArrayList<>();

    public Zhipu getZhipu() {
        return zhipu;
    }

    public Feishu getFeishu() {
        return feishu;
    }

    public Report getReport() {
        return report;
    }

    public List<String> getSourceWhitelist() {
        return sourceWhitelist;
    }

    public void setSourceWhitelist(List<String> sourceWhitelist) {
        this.sourceWhitelist = sourceWhitelist;
    }

    public static class Zhipu {
        @NotBlank
        private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";
        private String apiKey;
        @NotBlank
        private String model = "glm-5";
        @Min(0)
        @Max(1)
        private double temperature = 1.0;
        @Min(1)
        @Max(131072)
        private int maxTokens = 65536;
        @NotBlank
        private String thinking = "enabled";
        @Min(0)
        @Max(5)
        private int maxRetries = 2;
        @Min(100)
        @Max(30000)
        private int retryBackoffMs = 2000;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public String getThinking() {
            return thinking;
        }

        public void setThinking(String thinking) {
            this.thinking = thinking;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public int getRetryBackoffMs() {
            return retryBackoffMs;
        }

        public void setRetryBackoffMs(int retryBackoffMs) {
            this.retryBackoffMs = retryBackoffMs;
        }
    }

    public static class Feishu {
        private String appId;
        private String appSecret;
        private String chatId;
//        private String webhookUrl;
        private boolean enableCloudDoc;
        private String cloudDocFolderToken;
        private String docxUrlPrefix;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public String getChatId() {
            return chatId;
        }

        public void setChatId(String chatId) {
            this.chatId = chatId;
        }

//        public String getWebhookUrl() {
//            return webhookUrl;
//        }

//        public void setWebhookUrl(String webhookUrl) {
//            this.webhookUrl = webhookUrl;
//        }

        public boolean isEnableCloudDoc() {
            return enableCloudDoc;
        }

        public void setEnableCloudDoc(boolean enableCloudDoc) {
            this.enableCloudDoc = enableCloudDoc;
        }

        public String getCloudDocFolderToken() {
            return cloudDocFolderToken;
        }

        public void setCloudDocFolderToken(String cloudDocFolderToken) {
            this.cloudDocFolderToken = cloudDocFolderToken;
        }

        public String getDocxUrlPrefix() {
            return docxUrlPrefix;
        }

        public void setDocxUrlPrefix(String docxUrlPrefix) {
            this.docxUrlPrefix = docxUrlPrefix;
        }
    }

    public static class Report {
        @NotBlank
        private String cron = "0 0 9 * * *";
        @NotBlank
        private String zoneId = "Asia/Shanghai";
        @Min(1)
        @Max(6)
        private int loopRounds = 3;
        @Min(0)
        @Max(3)
        private int auditLoops = 1;
        @Min(1)
        @Max(8)
        private int maxItemsPerSection = 3;

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public String getZoneId() {
            return zoneId;
        }

        public void setZoneId(String zoneId) {
            this.zoneId = zoneId;
        }

        public int getLoopRounds() {
            return loopRounds;
        }

        public void setLoopRounds(int loopRounds) {
            this.loopRounds = loopRounds;
        }

        public int getMaxItemsPerSection() {
            return maxItemsPerSection;
        }

        public void setMaxItemsPerSection(int maxItemsPerSection) {
            this.maxItemsPerSection = maxItemsPerSection;
        }

        public int getAuditLoops() {
            return auditLoops;
        }

        public void setAuditLoops(int auditLoops) {
            this.auditLoops = auditLoops;
        }
    }
}
