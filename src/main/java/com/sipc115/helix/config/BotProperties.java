package com.sipc115.helix.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 机器人配置类
 * <p>
 * 用于读取和管理应用配置
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Validated
@ConfigurationProperties(prefix = "app")
public class BotProperties {

    /**
     * 智谱配置
     */
    private final Zhipu zhipu = new Zhipu();
    
    /**
     * 飞书配置
     */
    private final Feishu feishu = new Feishu();
    
    /**
     * 报告配置
     */
    private final Report report = new Report();
    
    /**
     * 源白名单
     */
    private List<String> sourceWhitelist = new ArrayList<>();

    /**
     * 获取智谱配置
     * 
     * @return 智谱配置
     */
    public Zhipu getZhipu() {
        return zhipu;
    }

    /**
     * 获取飞书配置
     * 
     * @return 飞书配置
     */
    public Feishu getFeishu() {
        return feishu;
    }

    /**
     * 获取报告配置
     * 
     * @return 报告配置
     */
    public Report getReport() {
        return report;
    }

    /**
     * 获取源白名单
     * 
     * @return 源白名单
     */
    public List<String> getSourceWhitelist() {
        return sourceWhitelist;
    }

    /**
     * 设置源白名单
     * 
     * @param sourceWhitelist 源白名单
     */
    public void setSourceWhitelist(List<String> sourceWhitelist) {
        this.sourceWhitelist = sourceWhitelist;
    }

    /**
     * 智谱配置类
     */
    public static class Zhipu {
        /**
         * 基础 URL
         */
        @NotBlank
        private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";
        
        /**
         * API 密钥
         */
        private String apiKey;
        
        /**
         * 模型名称
         */
        @NotBlank
        private String model = "glm-5";
        
        /**
         * 温度参数
         */
        @Min(0)
        @Max(1)
        private double temperature = 1.0;
        
        /**
         * 最大 token 数
         */
        @Min(1)
        @Max(131072)
        private int maxTokens = 65536;
        
        /**
         * 思考模式
         */
        @NotBlank
        private String thinking = "enabled";
        
        /**
         * 最大重试次数
         */
        @Min(0)
        @Max(5)
        private int maxRetries = 2;
        
        /**
         * 重试退避时间（毫秒）
         */
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

    /**
     * 飞书配置类
     */
    public static class Feishu {
        /**
         * 应用 ID
         */
        private String appId;
        
        /**
         * 应用密钥
         */
        private String appSecret;
        
        /**
         * 聊天 ID
         */
        private String chatId;
//        private String webhookUrl;
        
        /**
         * 是否启用云文档
         */
        private boolean enableCloudDoc;
        
        /**
         * 云文档文件夹令牌
         */
        private String cloudDocFolderToken;
        
        /**
         * 文档 URL 前缀
         */
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

    /**
     * 报告配置类
     */
    public static class Report {
        /**
         * 定时任务表达式
         */
        @NotBlank
        private String cron = "0 0 9 * * *";
        
        /**
         * 时区 ID
         */
        @NotBlank
        private String zoneId = "Asia/Shanghai";
        
        /**
         * 循环轮次
         */
        @Min(1)
        @Max(6)
        private int loopRounds = 3;
        
        /**
         * 审计轮次
         */
        @Min(0)
        @Max(3)
        private int auditLoops = 1;
        
        /**
         * 每个部分的最大项目数
         */
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
