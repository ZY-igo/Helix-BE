package com.sipc115.helix.integration;

import com.sipc115.helix.Application;
import com.sipc115.helix.domain.integration.IntegrationConnection;
import com.sipc115.helix.repository.jpa.IntegrationConnectionRepository;
import com.sipc115.helix.service.integration.EncryptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@Transactional
class WorkflowIntegrationTest {

    @Autowired
    private IntegrationConnectionRepository connectionRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Test
    @DisplayName("测试完整的工作流连接创建流程")
    void testCreateAndRetrieveConnection() {
        // 创建飞书连接
        IntegrationConnection feishuConn = IntegrationConnection.builder()
                .name("测试飞书连接")
                .type(IntegrationConnection.Type.FEISHU)
                .category(IntegrationConnection.Category.IM)
                .config(Map.of(
                        "appId", "cli_test123",
                        "appSecret", encryptionService.encrypt("test-secret"),
                        "apiBaseUrl", "https://open.feishu.cn/open-apis"
                ))
                .status(IntegrationConnection.Status.ACTIVE)
                .description("用于测试的飞书连接")
                .createdBy("test-user")
                .build();

        IntegrationConnection saved = connectionRepository.save(feishuConn);

        // 验证保存成功
        assertNotNull(saved.getId());
        assertEquals("测试飞书连接", saved.getName());

        // 验证可以查询
        IntegrationConnection retrieved = connectionRepository.findById(saved.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals(IntegrationConnection.Type.FEISHU, retrieved.getType());
        assertNotNull(retrieved.getCreatedAt());
        assertNotNull(retrieved.getUpdatedAt());
    }

    @Test
    @DisplayName("测试加密解密集成")
    void testEncryptionIntegration() {
        String originalSecret = "my-secret-api-key-12345";
        String encrypted = encryptionService.encrypt(originalSecret);
        String decrypted = encryptionService.decrypt(encrypted);

        assertNotNull(encrypted);
        assertNotEquals(originalSecret, encrypted);
        assertEquals(originalSecret, decrypted);
    }

    @Test
    @DisplayName("测试按类型查询连接")
    void testFindByType() {
        // 创建多个连接
        IntegrationConnection feishu1 = createFeishuConnection("飞书1");
        IntegrationConnection feishu2 = createFeishuConnection("飞书2");
        IntegrationConnection llm = createLlmConnection("智谱AI");

        connectionRepository.save(feishu1);
        connectionRepository.save(feishu2);
        connectionRepository.save(llm);

        // 查询飞书连接
        var feishuConnections = connectionRepository.findByType(IntegrationConnection.Type.FEISHU);
        assertEquals(2, feishuConnections.size());

        // 查询LLM连接
        var llmConnections = connectionRepository.findByType(IntegrationConnection.Type.LLM);
        assertEquals(1, llmConnections.size());
    }

    @Test
    @DisplayName("测试默认连接设置")
    void testDefaultConnection() {
        IntegrationConnection conn1 = createFeishuConnection("默认飞书");
        conn1.setIsDefault(true);
        connectionRepository.save(conn1);

        IntegrationConnection conn2 = createFeishuConnection("普通飞书");
        connectionRepository.save(conn2);

        // 查询默认连接
        var defaultConn = connectionRepository.findByTypeAndIsDefaultTrue(IntegrationConnection.Type.FEISHU);
        assertTrue(defaultConn.isPresent());
        assertEquals("默认飞书", defaultConn.get().getName());
    }

    private IntegrationConnection createFeishuConnection(String name) {
        return IntegrationConnection.builder()
                .name(name)
                .type(IntegrationConnection.Type.FEISHU)
                .category(IntegrationConnection.Category.IM)
                .config(Map.of("appId", "test-app-id"))
                .status(IntegrationConnection.Status.ACTIVE)
                .build();
    }

    private IntegrationConnection createLlmConnection(String name) {
        return IntegrationConnection.builder()
                .name(name)
                .type(IntegrationConnection.Type.LLM)
                .category(IntegrationConnection.Category.AI)
                .config(Map.of("apiKey", "test-api-key"))
                .status(IntegrationConnection.Status.ACTIVE)
                .build();
    }
}