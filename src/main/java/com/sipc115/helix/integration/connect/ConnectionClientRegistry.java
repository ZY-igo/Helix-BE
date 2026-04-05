/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.connect;

import com.sipc115.helix.domain.integration.IntegrationConnection;
import com.sipc115.helix.repository.jpa.IntegrationConnectionRepository;
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
    private final Map<Long, Object> instanceCache = new ConcurrentHashMap<>();
    private final IntegrationConnectionRepository connectionRepository;

    public ConnectionClientRegistry(List<ConnectionClient<?>> clientImpls,
                                   IntegrationConnectionRepository connectionRepository) {
        this.clientImpls = clientImpls;
        this.connectionRepository = connectionRepository;
        for (ConnectionClient<?> impl : clientImpls) {
            typeMap.put(impl.getConnectionType(), impl);
        }
        log.info("ConnectionClientRegistry initialized: {}", typeMap.keySet());
    }

    @SuppressWarnings("unchecked")
    public <T> ConnectionClient<T> getClientByType(String connectionType) {
        ConnectionClient<?> impl = typeMap.get(connectionType);
        if (impl == null) {
            throw new IllegalArgumentException("Unsupported connection type: " + connectionType);
        }
        return (ConnectionClient<T>) impl;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrCreateClient(Long connectionId, String connectionType, Object config) {
        Object cached = instanceCache.get(connectionId);
        if (cached != null) {
            return (T) cached;
        }

        ConnectionClient<T> client = getClientByType(connectionType);
        T instance;
        try {
            instance = client.createClient(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create client for connection: " + connectionId, e);
        }

        instanceCache.put(connectionId, instance);
        log.debug("Created and cached client: connectionId={}, type={}", connectionId, connectionType);
        return instance;
    }

    public <T> T getOrCreateClientByConnection(Long connectionId) {
        IntegrationConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("连接不存在: " + connectionId));

        return getOrCreateClient(connectionId, connection.getType(), connection.getConfig());
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