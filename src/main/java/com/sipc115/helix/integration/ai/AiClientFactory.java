package com.sipc115.helix.integration.ai;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AI 客户端工厂
 * <p>
 * 统一管理多个 AI 提供商，支持运行时动态切换
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Component
public class AiClientFactory {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(AiClientFactory.class);

    /**
     * 客户端映射，键为客户端类型，值为客户端实例
     */
    private final Map<String, AiClient> clientMap = new ConcurrentHashMap<>();

    /**
     * 构造函数，自动注入所有 AiClient 实现
     * 
     * @param clients AI 客户端列表
     */
    public AiClientFactory(List<AiClient> clients) {
        log.info("[AiClientFactory] Initializing with {} AI clients", clients.size());
        for (AiClient client : clients) {
            String type = client.getClientType();
            clientMap.put(type, client);
            log.info("[AiClientFactory] Registered AI client: {}", type);
        }
        log.info("[AiClientFactory] Available clients: {}", clientMap.keySet());
    }

    /**
     * 获取指定类型的 AI 客户端
     * 
     * @param clientType 客户端类型（如 "zhipu", "azure-openai"）
     * @return AI 客户端实例
     * @throws IllegalArgumentException 如果客户端类型不存在
     */
    public AiClient getClient(String clientType) {
        AiClient client = clientMap.get(clientType);
        if (client == null) {
            throw new IllegalArgumentException(
                "Unknown AI client type: " + clientType +
                ". Available types: " + clientMap.keySet()
            );
        }
        log.debug("[AiClientFactory] Returning client: {}", clientType);
        return client;
    }

    /**
     * 获取默认的 AI 客户端（默认使用智谱）
     * 
     * @return 默认客户端实例
     */
    public AiClient getDefaultClient() {
        return getClient("zhipu");
    }

    /**
     * 获取所有可用的客户端
     * 
     * @return 客户端列表（不可变）
     */
    public List<AiClient> getAllClients() {
        return List.copyOf(clientMap.values());
    }

    /**
     * 获取所有可用的客户端类型
     * 
     * @return 类型名称数组
     */
    public String[] getAvailableTypes() {
        return clientMap.keySet().toArray(new String[0]);
    }

    /**
     * 检查某个类型是否可用
     * 
     * @param clientType 客户端类型
     * @return 是否可用
     */
    public boolean hasClient(String clientType) {
        return clientMap.containsKey(clientType);
    }

    /**
     * 动态注册新的 AI 客户端（支持热部署）
     * 
     * @param client AI 客户端实例
     */
    public void registerClient(AiClient client) {
        String type = client.getClientType();
        clientMap.put(type, client);
        log.info("[AiClientFactory] Dynamically registered new client: {}", type);
    }

    /**
     * 注销 AI 客户端
     * 
     * @param clientType 客户端类型
     * @return 被注销的客户端，如果不存在则返回 null
     */
    public AiClient unregisterClient(String clientType) {
        AiClient removed = clientMap.remove(clientType);
        if (removed != null) {
            log.info("[AiClientFactory] Unregistered client: {}", clientType);
        }
        return removed;
    }
}
