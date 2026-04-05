package com.sipc115.helix.integration.connect;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConnectionClientRegistry {

    private final List<ConnectionClient<?>> clientImpls;
    private final Map<String, ConnectionClient<?>> typeMap = new ConcurrentHashMap<>();
    
    // 按 connectionId 缓存已创建的客户端实例
    private final Map<Long, Object> instanceCache = new ConcurrentHashMap<>();

    public ConnectionClientRegistry(List<ConnectionClient<?>> clientImpls) {
        this.clientImpls = clientImpls;
        for (ConnectionClient<?> impl : clientImpls) {
            typeMap.put(impl.getConnectionType(), impl);
        }
        log.info("ConnectionClientRegistry initialized: {}", typeMap.keySet());
    }

    @SuppressWarnings("unchecked")
    public <T> ConnectionClient<T> getClientByType(String connectionType) {
        ConnectionClient<?> impl = typeMap.get(connectionType);
        if (impl == null) {
            throw new IllegalArgumentException("Unsupported type: " + connectionType);
        }
        return (ConnectionClient<T>) impl;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrCreateClient(Long connectionId, String connectionType, Object config) throws Exception {
        Object cached = instanceCache.get(connectionId);
        if (cached != null) {
            return (T) cached;
        }

        ConnectionClient<T> client = getClientByType(connectionType);
        T instance = client.createClient(config);
        
        instanceCache.put(connectionId, instance);
        log.debug("Created and cached client: connectionId={}, type={}", connectionId, connectionType);
        return instance;
    }

    public void evictClient(Long connectionId) {
        instanceCache.remove(connectionId);
        log.debug("Evicted client cache: connectionId={}", connectionId);
    }

    public boolean hasType(String connectionType) {
        return typeMap.containsKey(connectionType);
    }

    public List<String> getSupportedTypes() {
        return List.copyOf(typeMap.keySet());
    }
}
