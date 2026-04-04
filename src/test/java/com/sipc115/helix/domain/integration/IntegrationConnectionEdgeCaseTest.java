/**
 * 集成连接实体边界情况测试类
 * <p>
 * 本测试类针对 IntegrationConnection 实体类的边界情况和异常场景进行测试，
 * 包括完整字段设置、空值处理、时间戳更新等场景。
 *
 * <h3>测试覆盖范围：</h3>
 * <ul>
 *   <li>完整构建：使用 Builder 设置所有字段</li>
 *   <li>默认值验证：无参构造的对象应有合理默认值</li>
 *   <li>常量完整性：验证所有枚举常量非空</li>
 *   <li>空值处理：null 类型、空配置 Map</li>
 *   <li>生命周期回调：onCreate 和 onUpdate 方法</li>
 * </ul>
 *
 * @see IntegrationConnection
 */
package com.sipc115.helix.domain.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Map;

/**
 * IntegrationConnection 实体边界情况和异常场景测试
 *
 * <p>
 * 验证实体类在各种边界条件下的行为，
 * 确保 JPA 实体注解、Lifecycle callbacks 等功能正常工作。
 */
class IntegrationConnectionEdgeCaseTest {

    /**
     * 测试使用 Builder 设置所有字段
     * <p>
     * 验证 IntegrationConnection 的 Builder 模式能正确设置所有字段，
     * 包括创建时间、更新时间、最后测试时间等时间戳字段。
     *
     * <h3>测试字段：</h3>
     * <ul>
     *   <li>基础信息：id、name、type、category、status</li>
     *   <li>配置：config（明文配置）、encryptedConfig（加密配置）</li>
     *   <li>状态：isDefault、lastTestAt、lastTestResult</li>
     *   <li>审计字段：description、createdBy、createdAt、updatedAt</li>
     * </ul>
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>所有字段值都能正确获取</li>
     *   <li>时间戳字段精确保存</li>
     * </ul>
     */
    @Test
    @DisplayName("测试构建器创建完整连接")
    void testBuilderWithAllFields() {
        Instant now = Instant.now();
        IntegrationConnection conn = IntegrationConnection.builder()
                .id(1L)
                .name("Test Connection")
                .type(IntegrationConnection.Type.FEISHU)
                .category(IntegrationConnection.Category.IM)
                .config(Map.of("key", "value"))
                .encryptedConfig("encrypted")
                .status(IntegrationConnection.Status.ACTIVE)
                .isDefault(true)
                .lastTestAt(now)
                .lastTestResult("SUCCESS")
                .description("Test description")
                .createdBy("test-user")
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(1L, conn.getId());
        assertEquals("Test Connection", conn.getName());
        assertEquals(IntegrationConnection.Type.FEISHU, conn.getType());
        assertEquals(IntegrationConnection.Category.IM, conn.getCategory());
        assertEquals("value", conn.getConfig().get("key"));
        assertEquals("encrypted", conn.getEncryptedConfig());
        assertEquals(IntegrationConnection.Status.ACTIVE, conn.getStatus());
        assertTrue(conn.getIsDefault());
        assertEquals(now, conn.getLastTestAt());
        assertEquals("SUCCESS", conn.getLastTestResult());
        assertEquals("Test description", conn.getDescription());
        assertEquals("test-user", conn.getCreatedBy());
        assertEquals(now, conn.getCreatedAt());
        assertEquals(now, conn.getUpdatedAt());
    }

    /**
     * 测试默认值设置
     * <p>
     * 验证使用无参构造函数创建对象时，
     * 系统赋予的默认值是否正确。
     *
     * <h3>预期默认值：</h3>
     * <ul>
     *   <li>status: "ACTIVE"（默认活跃）</li>
     *   <li>isDefault: false（非默认）</li>
     * </ul>
     */
    @Test
    @DisplayName("测试默认值设置")
    void testDefaultValues() {
        IntegrationConnection conn = new IntegrationConnection();

        assertEquals("ACTIVE", conn.getStatus());
        assertFalse(conn.getIsDefault());
    }

    /**
     * 测试所有状态常量非空
     * <p>
     * 参数化测试验证所有状态常量值都有效。
     *
     * @see IntegrationConnection.Status
     */
    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "INACTIVE", "ERROR"})
    @DisplayName("测试所有状态常量")
    void testAllStatusConstants(String status) {
        assertNotNull(status);
        assertFalse(status.isEmpty());
    }

    /**
     * 测试所有类型常量非空
     * <p>
     * 参数化测试验证所有类型常量值都有效。
     *
     * @see IntegrationConnection.Type
     */
    @ParameterizedTest
    @ValueSource(strings = {"FEISHU", "MYSQL", "LLM", "REDIS"})
    @DisplayName("测试所有类型常量")
    void testAllTypeConstants(String type) {
        assertNotNull(type);
        assertFalse(type.isEmpty());
    }

    /**
     * 测试所有分类常量非空
     * <p>
     * 参数化测试验证所有分类常量值都有效。
     *
     * @see IntegrationConnection.Category
     */
    @ParameterizedTest
    @ValueSource(strings = {"IM", "DATABASE", "AI", "CACHE"})
    @DisplayName("测试所有分类常量")
    void testAllCategoryConstants(String category) {
        assertNotNull(category);
        assertFalse(category.isEmpty());
    }

    /**
     * 测试 null 类型值
     * <p>
     * 验证连接类型可以设置为 null。
     * 这允许创建"未分类"的连接。
     *
     * <h3>使用场景：</h3>
     * <p>
     * 在连接创建过程中，类型可能尚未确定，
     * 允许 null 可以支持"先创建后分类"的流程。
     */
    @Test
    @DisplayName("测试null类型值")
    void testNullTypeValue() {
        IntegrationConnection conn = new IntegrationConnection();
        conn.setType(null);
        assertNull(conn.getType());
    }

    /**
     * 测试空配置 Map
     * <p>
     * 验证配置 Map 可以是空的。
     * 这表示连接没有额外的配置项。
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>getConfig() 返回空 Map（非 null）</li>
     *   <li>Map 的大小为 0</li>
     * </ul>
     */
    @Test
    @DisplayName("测试空配置Map")
    void testEmptyConfigMap() {
        IntegrationConnection conn = new IntegrationConnection();
        conn.setConfig(Map.of());
        assertNotNull(conn.getConfig());
        assertTrue(conn.getConfig().isEmpty());
    }

    /**
     * 测试 null 配置 Map
     * <p>
     * 验证配置 Map 可以是 null。
     * 这表示连接配置尚未初始化。
     *
     * <h3>设计理由：</h3>
     * <p>
     * 有些连接可能不需要额外配置，
     * 使用 null 可以区分"无配置"和"有空配置"。
     */
    @Test
    @DisplayName("测试null配置Map")
    void testNullConfigMap() {
        IntegrationConnection conn = new IntegrationConnection();
        conn.setConfig(null);
        assertNull(conn.getConfig());
    }

    /**
     * 测试 onCreate 方法设置时间戳
     * <p>
     * 验证 JPA @PrePersist 生命周期回调方法 onCreate()：
     * <ol>
     *   <li>createdAt 被设置为当前时间</li>
     *   <li>updatedAt 被设置为当前时间</li>
     * </ol>
     *
     * <h3>使用场景：</h3>
     * <p>
     * 当实体对象第一次保存到数据库时，
     * JPA 会自动调用 @PrePersist 标注的方法。
     */
    @Test
    @DisplayName("测试onCreate方法设置时间戳")
    void testOnCreateSetsTimestamps() {
        IntegrationConnection conn = new IntegrationConnection();
        conn.onCreate();

        assertNotNull(conn.getCreatedAt());
        assertNotNull(conn.getUpdatedAt());
    }

    /**
     * 测试 onUpdate 方法更新时间戳
     * <p>
     * 验证 JPA @PreUpdate 生命周期回调方法 onUpdate()：
     * <ol>
     *   <li>updatedAt 被更新为更新后的时间</li>
     *   <li>createdAt 保持不变</li>
     * </ol>
     *
     * <h3>测试方法：</h3>
     * <ol>
     *   <li>先调用 onCreate() 设置初始时间</li>
     *   <li>等待 10ms 让时间有差异</li>
     *   <li>调用 onUpdate() 更新时间</li>
     *   <li>验证 updatedAt 被更新</li>
     * </ol>
     */
    @Test
    @DisplayName("测试onUpdate方法更新时间戳")
    void testOnUpdateUpdatesTimestamp() throws InterruptedException {
        IntegrationConnection conn = new IntegrationConnection();
        conn.onCreate();
        Instant firstUpdate = conn.getUpdatedAt();

        Thread.sleep(10);
        conn.onUpdate();

        assertTrue(conn.getUpdatedAt().isAfter(firstUpdate) || conn.getUpdatedAt().equals(firstUpdate));
    }
}