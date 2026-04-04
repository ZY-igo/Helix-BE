/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.controller.ConnectionTestResult;
import com.sipc115.helix.controller.IntegrationConnectionRequest;
import com.sipc115.helix.domain.integration.IntegrationConnection;
import com.sipc115.helix.domain.integration.IntegrationConnectionHistory;
import com.sipc115.helix.repository.jpa.IntegrationConnectionHistoryRepository;
import com.sipc115.helix.repository.jpa.IntegrationConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationConnectionService {

    private final IntegrationConnectionRepository connectionRepository;
    private final IntegrationConnectionHistoryRepository historyRepository;
    private final EncryptionService encryptionService;
    private final IntegrationConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;

    public List<IntegrationConnection> getAllConnections() {
        return connectionRepository.findAll();
    }

    public Optional<IntegrationConnection> getConnection(Long id) {
        return connectionRepository.findById(id);
    }

    public List<IntegrationConnection> getConnectionsByType(String type) {
        return connectionRepository.findByType(type);
    }

    public List<IntegrationConnection> getConnectionsByCategory(String category) {
        return connectionRepository.findByCategory(category);
    }

    @Transactional
    public IntegrationConnection createConnection(IntegrationConnectionRequest request) {
        IntegrationConnection conn = new IntegrationConnection();
        conn.setName(request.getName());
        conn.setType(request.getType());
        conn.setCategory(request.getCategory());
        conn.setConfig(encryptSensitiveFields(request.getConfig(), request.getType()));
        conn.setDescription(request.getDescription());
        conn.setStatus(IntegrationConnection.Status.ACTIVE);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            connectionRepository.clearDefaultForType(request.getType());
            conn.setIsDefault(true);
        }

        IntegrationConnection saved = connectionRepository.save(conn);
        saveHistory(saved, IntegrationConnectionHistory.ChangeType.CREATE, "system");

        connectionFactory.evictConnection(saved.getId());
        log.info("创建连接: id={}, name={}, type={}", saved.getId(), saved.getName(), saved.getType());

        return saved;
    }

    @Transactional
    public Optional<IntegrationConnection> updateConnection(Long id, IntegrationConnectionRequest request) {
        return connectionRepository.findById(id).map(conn -> {
            conn.setName(request.getName());
            conn.setType(request.getType());
            conn.setCategory(request.getCategory());
            conn.setConfig(encryptSensitiveFields(request.getConfig(), request.getType()));
            conn.setDescription(request.getDescription());

            if (Boolean.TRUE.equals(request.getIsDefault())) {
                connectionRepository.clearDefaultForType(request.getType());
                conn.setIsDefault(true);
            }

            IntegrationConnection updated = connectionRepository.save(conn);
            saveHistory(updated, IntegrationConnectionHistory.ChangeType.UPDATE, "system");

            connectionFactory.evictConnection(updated.getId());
            log.info("更新连接: id={}, name={}", updated.getId(), updated.getName());

            return updated;
        });
    }

    @Transactional
    public void deleteConnection(Long id) {
        connectionRepository.findById(id).ifPresent(conn -> {
            saveHistory(conn, IntegrationConnectionHistory.ChangeType.DELETE, "system");
            connectionRepository.delete(conn);
            connectionFactory.evictConnection(id);
            log.info("删除连接: id={}, name={}", id, conn.getName());
        });
    }

    @Transactional
    public ConnectionTestResult testConnection(Long id) {
        return connectionRepository.findById(id).map(conn -> {
            ConnectionTestResult result;
            try {
                switch (conn.getType()) {
                    case IntegrationConnection.Type.FEISHU -> testFeishuConnection(conn);
                    case IntegrationConnection.Type.LLM -> testLlmConnection(conn);
                    default -> throw new UnsupportedOperationException("不支持的连接类型: " + conn.getType());
                }

                conn.setLastTestAt(Instant.now());
                conn.setLastTestResult("SUCCESS");
                connectionRepository.save(conn);
                saveHistory(conn, IntegrationConnectionHistory.ChangeType.TEST, "system");

                result = ConnectionTestResult.builder()
                        .success(true)
                        .message("连接测试成功")
                        .connectionId(conn.getId())
                        .connectionName(conn.getName())
                        .connectionType(conn.getType())
                        .build();

            } catch (Exception e) {
                conn.setLastTestAt(Instant.now());
                conn.setLastTestResult("FAILED");
                conn.setStatus(IntegrationConnection.Status.ERROR);
                connectionRepository.save(conn);

                result = ConnectionTestResult.builder()
                        .success(false)
                        .message("连接测试失败: " + e.getMessage())
                        .connectionId(conn.getId())
                        .connectionName(conn.getName())
                        .connectionType(conn.getType())
                        .build();
            }

            return result;
        }).orElse(ConnectionTestResult.builder()
                .success(false)
                .message("连接不存在")
                .build());
    }

    @Transactional
    public Optional<IntegrationConnection> setDefaultConnection(Long id) {
        return connectionRepository.findById(id).map(conn -> {
            connectionRepository.clearDefaultForType(conn.getType());
            conn.setIsDefault(true);
            IntegrationConnection updated = connectionRepository.save(conn);
            log.info("设置默认连接: id={}, name={}, type={}", updated.getId(), updated.getName(), updated.getType());
            return updated;
        });
    }

    private void testFeishuConnection(IntegrationConnection conn) {
        IntegrationConnectionFactory.FeishuClientProxy client = connectionFactory.createFeishuClient(conn);
        client.sendText("test_chat_id", "Connection test message");
    }

    private void testLlmConnection(IntegrationConnection conn) {
        IntegrationConnectionFactory.ZhipuClient client = connectionFactory.createZhipuClient(conn);
        client.chat("test", "You are a helpful assistant.", "Say 'OK' if you receive this message.");
    }

    private Map<String, Object> encryptSensitiveFields(Map<String, Object> config, String type) {
        if (config == null) {
            return null;
        }

        Map<String, Object> mutableConfig = new java.util.HashMap<>(config);

        switch (type) {
            case IntegrationConnection.Type.FEISHU -> {
                if (mutableConfig.containsKey("appSecret")) {
                    mutableConfig.put("appSecret", encryptionService.encrypt((String) mutableConfig.get("appSecret")));
                }
            }
            case IntegrationConnection.Type.LLM -> {
                if (mutableConfig.containsKey("apiKey")) {
                    mutableConfig.put("apiKey", encryptionService.encrypt((String) mutableConfig.get("apiKey")));
                }
            }
            case IntegrationConnection.Type.MYSQL -> {
                if (mutableConfig.containsKey("password")) {
                    mutableConfig.put("password", encryptionService.encrypt((String) mutableConfig.get("password")));
                }
            }
        }

        return mutableConfig;
    }

    private void saveHistory(IntegrationConnection conn, String changeType, String changedBy) {
        IntegrationConnectionHistory history = new IntegrationConnectionHistory();
        history.setConnectionId(conn.getId());
        history.setName(conn.getName());
        history.setType(conn.getType());
        history.setConfig(conn.getConfig());
        history.setEncryptedConfig(conn.getEncryptedConfig());
        history.setStatus(conn.getStatus());
        history.setChangeType(changeType);
        history.setChangedBy(changedBy);
        historyRepository.save(history);
    }
}