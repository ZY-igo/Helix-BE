package com.sipc115.helix.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
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
@Getter
@Validated
@ConfigurationProperties(prefix = "app")
public class BotProperties {

    /**
     * 智谱配置
     * -- GETTER --
     *  获取智谱配置
     *
     * @return 智谱配置

     */
    private final Zhipu zhipu = new Zhipu();
    
    /**
     * 飞书配置
     * -- GETTER --
     *  获取飞书配置
     *
     * @return 飞书配置

     */
    private final Feishu feishu = new Feishu();
    
    /**
     * 报告配置
     * -- GETTER --
     *  获取报告配置
     *
     * @return 报告配置

     */
    private final Report report = new Report();

    /**
     * 智谱配置类
     */
    @Setter
    @Getter
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

    }

    /**
     * 飞书配置类
     */
    @Setter
    @Getter
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

        //        public String getWebhookUrl() {
//            return webhookUrl;
//        }

//        public void setWebhookUrl(String webhookUrl) {
//            this.webhookUrl = webhookUrl;
//        }

    }

    /**
     * 报告配置类
     */
    @Setter
    @Getter
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

    }
}
