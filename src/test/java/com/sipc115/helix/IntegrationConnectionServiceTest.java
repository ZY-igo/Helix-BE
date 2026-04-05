package com.sipc115.helix;

import com.sipc115.helix.domain.integration.IntegrationConnection;
import com.sipc115.helix.integration.connect.IntegrationConnectionService;
import com.sipc115.helix.repository.jpa.IntegrationConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成连接服务层测试类
 * <p>
 * 测试 IntegrationConnectionService 的核心功能，包括连接的创建、查询、更新、删除和连通性测试。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class IntegrationConnectionServiceTest {

    @Autowired
    private IntegrationConnectionService connectionService;

    @Autowired
    private IntegrationConnectionRepository connectionRepository;

    private Long llmConnectionId;
    private Long feishuConnectionId;

    /**
     * 测试前置准备：创建 LLM 和飞书测试连接
     */
    @BeforeEach
    void setUp() {
        // 创建 LLM 测试连接
        Map<String, Object> llmConfig = Map.of(
                "apiKey", "test-api-key-123",
                "baseUrl", "https://open.bigmodel.cn/api/paas/v4",
                "model", "glm-4"
        );
        IntegrationConnection llmConn = connectionService.createConnection("测试LLM连接", "LLM", llmConfig);
        llmConnectionId = llmConn.getId();

        // 创建飞书测试连接
        Map<String, Object> feishuConfig = Map.of(
                "appId", "cli_test_app_id",
                "appSecret", "test_secret_123"
        );
        IntegrationConnection feishuConn = connectionService.createConnection("测试飞书连接", "FEISHU", feishuConfig);
        feishuConnectionId = feishuConn.getId();
    }

    /**
     * 测试创建连接功能
     */
    @Test
    void testCreateConnection() {
        Map<String, Object> config = Map.of("key", "value");
        IntegrationConnection conn = connectionService.createConnection("新连接", "LLM", config);

        assertNotNull(conn.getId(), "连接 ID 不应为空");
        assertEquals("新连接", conn.getName(), "连接名称应匹配");
        assertEquals("LLM", conn.getType(), "连接类型应匹配");
        assertEquals(IntegrationConnection.Status.ACTIVE, conn.getStatus(), "连接状态应为 ACTIVE");
    }

    /**
     * 测试创建不支持的连接类型时应抛出异常
     */
    @Test
    void testCreateConnectionWithUnsupportedType() {
        assertThrows(IllegalArgumentException.class, () -> {
            connectionService.createConnection("无效连接", "UNKNOWN_TYPE", Map.of());
        }, "创建不支持类型的连接应抛出 IllegalArgumentException");
    }

    /**
     * 测试根据 ID 获取连接
     */
    @Test
    void testGetConnection() {
        IntegrationConnection conn = connectionService.getConnection(llmConnectionId);
        assertNotNull(conn, "连接不应为空");
        assertEquals("测试LLM连接", conn.getName(), "连接名称应匹配");
        assertEquals("LLM", conn.getType(), "连接类型应匹配");
    }

    /**
     * 测试获取所有连接列表
     */
    @Test
    void testGetAllConnections() {
        List<IntegrationConnection> connections = connectionService.getAllConnections();
        assertTrue(connections.size() >= 2, "连接列表至少应包含 2 个测试连接");
    }

    /**
     * 测试根据类型筛选连接
     */
    @Test
    void testGetConnectionsByType() {
        List<IntegrationConnection> llmConnections = connectionService.getConnectionsByType("LLM");
        assertFalse(llmConnections.isEmpty(), "LLM 连接列表不应为空");
        llmConnections.forEach(conn ->
            assertEquals("LLM", conn.getType(), "筛选出的连接类型应全为 LLM")
        );
    }

    /**
     * 测试更新连接信息
     */
    @Test
    void testUpdateConnection() {
        Map<String, Object> newConfig = Map.of("apiKey", "new-key", "model", "glm-4-plus");
        IntegrationConnection updated = connectionService.updateConnection(llmConnectionId, "更新后的LLM", newConfig);

        assertEquals("更新后的LLM", updated.getName(), "连接名称应已更新");
        assertEquals("new-key", updated.getConfig().get("apiKey"), "配置中的 API Key 应已更新");
    }

    /**
     * 测试删除连接功能
     */
    @Test
    void testDeleteConnection() {
        Long tempId = connectionService.createConnection("临时连接", "LLM", Map.of("apiKey", "temp")).getId();

        connectionService.deleteConnection(tempId);

        assertThrows(IllegalArgumentException.class, () -> {
            connectionService.getConnection(tempId);
        }, "删除后的连接再次查询应抛出异常");
    }

    /**
     * 测试获取系统支持的连接类型列表
     */
    @Test
    void testGetSupportedTypes() {
        List<String> types = connectionService.getSupportedTypes();
        assertTrue(types.contains("LLM"), "支持的类型应包含 LLM");
        assertTrue(types.contains("FEISHU"), "支持的类型应包含 FEISHU");
    }

    /**
     * 测试连接连通性（注意：此测试会发起真实的 HTTP 请求）
     */
    @Test
    void testTestConnection() throws Exception {
        assertDoesNotThrow(() -> {
            connectionService.testConnection(llmConnectionId);
        }, "连通性测试不应抛出异常");
    }
}
