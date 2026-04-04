/**
 * 集成连接服务单元测试类
 * <p>
 * 本测试类针对 IntegrationConnectionService 的业务逻辑进行测试，
 * 验证连接管理服务的 CRUD 操作、加密处理和业务规则是否正确。
 *
 * <h3>测试范围：</h3>
 * <ul>
 *   <li>创建连接：创建飞书连接和LLM连接，验证配置加密</li>
 *   <li>查询连接：根据ID查询、类型查询、分类查询</li>
 *   <li>删除连接：删除连接并清除缓存</li>
 *   <li>默认连接：设置和清除默认连接</li>
 * </ul>
 *
 * <h3>测试策略：</h3>
 * <p>
 * 使用 Mockito 进行依赖注入模拟，
 * 验证服务层与仓储层、加密服务、连接工厂的交互是否正确。
 *
 * @see IntegrationConnectionService
 * @see IntegrationConnection
 */
package com.sipc115.helix.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.controller.ConnectionTestResult;
import com.sipc115.helix.controller.IntegrationConnectionRequest;
import com.sipc115.helix.domain.integration.IntegrationConnection;
import com.sipc115.helix.domain.integration.IntegrationConnectionHistory;
import com.sipc115.helix.repository.jpa.IntegrationConnectionHistoryRepository;
import com.sipc115.helix.repository.jpa.IntegrationConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 集成连接服务业务逻辑测试
 *
 * <p>
 * IntegrationConnectionService 是连接管理的核心服务，
 * 负责连接的创建、查询、更新、删除等操作。
 * 本测试类验证其业务逻辑的正确性。
 */
@ExtendWith(MockitoExtension.class)
class IntegrationConnectionServiceTest {

    @Mock
    private IntegrationConnectionRepository connectionRepository;

    @Mock
    private IntegrationConnectionHistoryRepository historyRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private IntegrationConnectionFactory connectionFactory;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IntegrationConnectionService connectionService;

    private IntegrationConnection feishuConnection;
    private IntegrationConnection llmConnection;

    /**
     * 测试前准备：创建测试用的连接对象
     * <p>
     * 初始化两个测试用连接：
     * <ul>
     *   <li>feishuConnection: 飞书连接，用于测试 IM 类连接</li>
     *   <li>llmConnection: LLM 连接，用于测试 AI 类连接</li>
     * </ul>
     */
    @BeforeEach
    void setUp() {
        feishuConnection = IntegrationConnection.builder()
                .id(1L)
                .name("飞书测试连接")
                .type(IntegrationConnection.Type.FEISHU)
                .category(IntegrationConnection.Category.IM)
                .config(Map.of("appId", "test-app-id", "appSecret", "encrypted-secret"))
                .status(IntegrationConnection.Status.ACTIVE)
                .build();

        llmConnection = IntegrationConnection.builder()
                .id(2L)
                .name("智谱AI测试连接")
                .type(IntegrationConnection.Type.LLM)
                .category(IntegrationConnection.Category.AI)
                .config(Map.of("apiKey", "encrypted-api-key", "model", "glm-5"))
                .status(IntegrationConnection.Status.ACTIVE)
                .build();
    }

    /**
     * 测试创建飞书连接
     * <p>
     * 验证创建飞书连接时：
     * <ol>
     *   <li>敏感配置（appSecret）会被加密</li>
     *   <li>调用仓储层保存连接</li>
     *   <li>清除同类型的其他默认连接</li>
     *   <li>记录连接历史</li>
     * </ol>
     *
     * <h3>测试步骤：</h3>
     * <ol>
     *   <li>创建飞书连接请求（包含明文 appSecret）</li>
     *   <li>模拟加密服务返回加密后的密文</li>
     *   <li>调用 createConnection 方法</li>
     *   <li>验证结果和交互</li>
     * </ol>
     */
    @Test
    @DisplayName("测试创建飞书连接")
    void testCreateFeishuConnection() {
        IntegrationConnectionRequest request = new IntegrationConnectionRequest();
        request.setName("飞书测试连接");
        request.setType(IntegrationConnection.Type.FEISHU);
        request.setCategory(IntegrationConnection.Category.IM);
        request.setConfig(Map.of("appId", "test-app-id", "appSecret", "secret123"));
        request.setIsDefault(true);

        when(encryptionService.encrypt("secret123")).thenReturn("encrypted-secret");
        when(connectionRepository.save(any())).thenReturn(feishuConnection);
        when(historyRepository.save(any())).thenReturn(new IntegrationConnectionHistory());

        IntegrationConnection result = connectionService.createConnection(request);

        assertNotNull(result);
        assertEquals("飞书测试连接", result.getName());
        verify(connectionRepository).clearDefaultForType(IntegrationConnection.Type.FEISHU);
        verify(connectionRepository).save(any());
    }

    /**
     * 测试创建LLM连接
     * <p>
     * 验证创建大语言模型连接时：
     * <ol>
     *   <li>敏感配置（apiKey）会被加密</li>
     *   <li>正确保存到数据库</li>
     * </ol>
     *
     * <h3>测试场景：</h3>
     * <p>
     * LLM 连接如智谱 AI，需要存储 apiKey 用于调用模型接口。
     * apiKey 作为敏感信息必须加密存储。
     */
    @Test
    @DisplayName("测试创建LLM连接")
    void testCreateLlmConnection() {
        IntegrationConnectionRequest request = new IntegrationConnectionRequest();
        request.setName("智谱AI测试连接");
        request.setType(IntegrationConnection.Type.LLM);
        request.setCategory(IntegrationConnection.Category.AI);
        request.setConfig(Map.of("apiKey", "api-key-123", "model", "glm-5"));

        when(encryptionService.encrypt("api-key-123")).thenReturn("encrypted-api-key");
        when(connectionRepository.save(any())).thenReturn(llmConnection);
        when(historyRepository.save(any())).thenReturn(new IntegrationConnectionHistory());

        IntegrationConnection result = connectionService.createConnection(request);

        assertNotNull(result);
        assertEquals("智谱AI测试连接", result.getName());
        verify(connectionRepository).save(any());
    }

    /**
     * 测试获取连接
     * <p>
     * 验证根据 ID 查询连接的功能：
     * <ol>
     *   <li>调用仓储层的 findById 方法</li>
     *   <li>返回存在的连接</li>
     * </ol>
     */
    @Test
    @DisplayName("测试获取连接")
    void testGetConnection() {
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(feishuConnection));

        Optional<IntegrationConnection> result = connectionService.getConnection(1L);

        assertTrue(result.isPresent());
        assertEquals("飞书测试连接", result.get().getName());
    }

    /**
     * 测试获取不存在的连接
     * <p>
     * 验证查询不存在的连接时：
     * <ol>
     *   <li>返回 Optional.empty()</li>
     *   <li>不会抛出异常</li>
     * </ol>
     *
     * <h3>设计理由：</h3>
     * <p>
     * 使用 Optional 让调用方明确处理"未找到"的情况，
     * 而不是抛出异常，这是更符合函数式编程风格的设计。
     */
    @Test
    @DisplayName("测试获取不存在的连接")
    void testGetNonExistentConnection() {
        when(connectionRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<IntegrationConnection> result = connectionService.getConnection(999L);

        assertFalse(result.isPresent());
    }

    /**
     * 测试删除连接
     * <p>
     * 验证删除连接时：
     * <ol>
     *   <li>先查询连接是否存在</li>
     *   <li>调用仓储层删除连接</li>
     *   <li>清除连接工厂中的缓存（如果有）</li>
     * </ol>
     *
     * <h3>缓存清除说明：</h3>
     * <p>
     * IntegrationConnectionFactory 可能缓存了连接客户端，
     * 删除连接时需要清除缓存，避免使用已删除的连接。
     */
    @Test
    @DisplayName("测试删除连接")
    void testDeleteConnection() {
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(feishuConnection));

        connectionService.deleteConnection(1L);

        verify(connectionRepository).delete(feishuConnection);
        verify(connectionFactory).evictConnection(1L);
    }

    /**
     * 测试设置默认连接
     * <p>
     * 验证将某个连接设置为默认连接时：
     * <ol>
     *   <li>先清除同类型的所有默认连接标记</li>
     *   <li>设置目标连接为默认</li>
     * </ol>
     *
     * <h3>业务规则：</h3>
     * <p>
     * 每种类型的连接只能有一个默认连接。
     * 设置新的默认连接时，系统自动取消同类型的其他默认连接。
     */
    @Test
    @DisplayName("测试设置默认连接")
    void testSetDefaultConnection() {
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(feishuConnection));
        when(connectionRepository.save(any())).thenReturn(feishuConnection);

        Optional<IntegrationConnection> result = connectionService.setDefaultConnection(1L);

        assertTrue(result.isPresent());
        verify(connectionRepository).clearDefaultForType(IntegrationConnection.Type.FEISHU);
    }

    /**
     * 测试按类型查询连接
     * <p>
     * 验证根据连接类型查询的功能。
     * 例如：查询所有飞书连接，用于工作流中选择通知服务。
     *
     * <h3>典型使用场景：</h3>
     * <ul>
     *   <li>用户在工作流中配置飞书通知节点</li>
     *   <li>系统列出所有飞书连接供用户选择</li>
     * </ul>
     */
    @Test
    @DisplayName("测试按类型查询连接")
    void testGetConnectionsByType() {
        when(connectionRepository.findByType(IntegrationConnection.Type.FEISHU))
                .thenReturn(java.util.List.of(feishuConnection));

        var result = connectionService.getConnectionsByType(IntegrationConnection.Type.FEISHU);

        assertEquals(1, result.size());
        assertEquals("飞书测试连接", result.get(0).getName());
    }

    /**
     * 测试按分类查询连接
     * <p>
     * 验证根据连接分类查询的功能。
     * 分类是对类型的进一步归类，方便筛选。
     *
     * <h3>分类示例：</h3>
     * <ul>
     *   <li>IM 分类下可能有：飞书、Slack、钉钉等</li>
     *   <li>AI 分类下可能有：智谱 AI、OpenAI、Azure OpenAI 等</li>
     * </ul>
     */
    @Test
    @DisplayName("测试按分类查询连接")
    void testGetConnectionsByCategory() {
        when(connectionRepository.findByCategory(IntegrationConnection.Category.IM))
                .thenReturn(java.util.List.of(feishuConnection));

        var result = connectionService.getConnectionsByCategory(IntegrationConnection.Category.IM);

        assertEquals(1, result.size());
        assertEquals(IntegrationConnection.Type.FEISHU, result.get(0).getType());
    }
}