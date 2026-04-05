package com.sipc115.helix.integration.connect;

import com.sipc115.helix.domain.integration.IntegrationConnection;
import com.sipc115.helix.repository.jpa.IntegrationConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationConnectionService {

    private final IntegrationConnectionRepository connectionRepo;
    private final ConnectionClientRegistry clientRegistry;

    @Transactional
    public IntegrationConnection createConnection(String name, String type, Map<String, Object> config) {
        if (!clientRegistry.hasType(type)) {
            throw new IllegalArgumentException("不支持的连接类型: " + type);
        }

        IntegrationConnection connection = new IntegrationConnection();
        connection.setName(name);
        connection.setType(type);
        connection.setConfig(config);
        connection.setStatus(IntegrationConnection.Status.ACTIVE);

        IntegrationConnection saved = connectionRepo.save(connection);
        log.info("创建集成连接: id={}, name={}, type={}", saved.getId(), saved.getName(), saved.getType());
        return saved;
    }

    @Transactional
    public IntegrationConnection updateConnection(Long id, String name, Map<String, Object> config) {
        IntegrationConnection connection = connectionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("连接不存在: " + id));

        if (name != null) {
            connection.setName(name);
        }
        if (config != null) {
            connection.setConfig(config);
        }

        IntegrationConnection updated = connectionRepo.save(connection);
        clientRegistry.evictClient(id);
        log.info("更新集成连接: id={}, name={}", updated.getId(), updated.getName());
        return updated;
    }

    @Transactional
    public void deleteConnection(Long id) {
        if (!connectionRepo.existsById(id)) {
            throw new IllegalArgumentException("连接不存在: " + id);
        }
        connectionRepo.deleteById(id);
        clientRegistry.evictClient(id);
        log.info("删除集成连接: id={}", id);
    }

    public List<IntegrationConnection> getAllConnections() {
        return connectionRepo.findAll();
    }

    public IntegrationConnection getConnection(Long id) {
        return connectionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("连接不存在: " + id));
    }

    public List<IntegrationConnection> getConnectionsByType(String type) {
        return connectionRepo.findByType(type);
    }

    public void testConnection(Long id) throws Exception {
        IntegrationConnection connection = getConnection(id);
        ConnectionClient<?> client = clientRegistry.getClientByType(connection.getType());
        client.test(connection.getConfig());
        log.info("连接测试成功: id={}, type={}", id, connection.getType());
    }

    public List<String> getSupportedTypes() {
        return clientRegistry.getSupportedTypes();
    }
}
