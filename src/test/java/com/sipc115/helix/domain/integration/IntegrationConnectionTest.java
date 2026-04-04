/**
 * 集成连接实体单元测试类
 * <p>
 * 本测试类针对 IntegrationConnection 实体类的创建、属性设置和默认值进行测试。
 * IntegrationConnection 是连接抽象层的核心实体，用于存储外部服务的连接配置。
 *
 * <h3>集成连接功能说明：</h3>
 * <ul>
 *   <li>支持多种外部服务的连接配置（飞书、MySQL、LLM、Redis等）</li>
 *   <li>敏感配置（如 API Key、密码）使用 AES-GCM 加密存储</li>
 *   <li>支持默认连接设置，方便快速引用</li>
 *   <li>记录连接测试历史，方便问题排查</li>
 * </ul>
 *
 * @see IntegrationConnection
 */
package com.sipc115.helix.domain.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Map;

/**
 * IntegrationConnection 实体类测试
 *
 * <p>
 * 测试 IntegrationConnection 的构建器模式、属性设置、常量值以及默认行为。
 * IntegrationConnection 使用 JPA 注解进行 ORM 映射，支持数据库持久化。
 */
class IntegrationConnectionTest {

    /**
     * 测试 IntegrationConnection 创建（使用构建器模式）
     * <p>
     * 验证使用 Builder 模式创建 IntegrationConnection 对象时，
     * 所有设置的属性都能正确存储和获取。
     *
     * <h3>测试的连接配置：</h3>
     * <ul>
     *   <li>ID: 1L</li>
     *   <li>名称: 飞书测试连接</li>
     *   <li>类型: FEISHU（飞书）</li>
     *   <li>分类: IM（即时通讯）</li>
     *   <li>配置: appId -> test-app-id</li>
     *   <li>状态: ACTIVE（活跃）</li>
     *   <li>是否默认: true</li>
     *   <li>描述: 测试描述</li>
     *   <li>创建者: admin</li>
     * </ul>
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>所有通过 builder 设置的属性值都能正确获取</li>
     *   <li>获取的值与设置的值完全一致</li>
     * </ul>
     */
    @Test
    @DisplayName("测试IntegrationConnection创建")
    void testIntegrationConnectionCreation() {
        IntegrationConnection conn = IntegrationConnection.builder()
                .id(1L)
                .name("飞书测试连接")
                .type(IntegrationConnection.Type.FEISHU)
                .category(IntegrationConnection.Category.IM)
                .config(Map.of("appId", "test-app-id"))
                .status(IntegrationConnection.Status.ACTIVE)
                .isDefault(true)
                .description("测试描述")
                .createdBy("admin")
                .build();

        assertEquals(1L, conn.getId());
        assertEquals("飞书测试连接", conn.getName());
        assertEquals(IntegrationConnection.Type.FEISHU, conn.getType());
        assertEquals(IntegrationConnection.Category.IM, conn.getCategory());
        assertEquals(IntegrationConnection.Status.ACTIVE, conn.getStatus());
        assertTrue(conn.getIsDefault());
    }

    /**
     * 测试状态常量值
     * <p>
     * 验证 IntegrationConnection.Status 枚举类的常量定义正确。
     *
     * <h3>状态说明：</h3>
     * <ul>
     *   <li>ACTIVE: 连接可用，正常工作</li>
     *   <li>INACTIVE: 连接被禁用，暂不可用</li>
     *   <li>ERROR: 连接异常，需要排查问题</li>
     * </ul>
     */
    @Test
    @DisplayName("测试状态常量")
    void testStatusConstants() {
        assertEquals("ACTIVE", IntegrationConnection.Status.ACTIVE);
        assertEquals("INACTIVE", IntegrationConnection.Status.INACTIVE);
        assertEquals("ERROR", IntegrationConnection.Status.ERROR);
    }

    /**
     * 测试类型常量值
     * <p>
     * 验证 IntegrationConnection.Type 枚举类的常量定义正确。
     *
     * <h3>类型说明：</h3>
     * <ul>
     *   <li>FEISHU: 飞书平台（用于发送消息、审批等）</li>
     *   <li>MYSQL: MySQL 数据库连接</li>
     *   <li>LLM: 大语言模型服务（如智谱 AI）</li>
     *   <li>REDIS: Redis 缓存服务</li>
     * </ul>
     *
     * <h3>设计理由：</h3>
     * <p>
     * 使用 String 类型的枚举而非 Java 枚举，
     * 是为了便于数据库存储和与外部系统集成。
     */
    @Test
    @DisplayName("测试类型常量")
    void testTypeConstants() {
        assertEquals("FEISHU", IntegrationConnection.Type.FEISHU);
        assertEquals("MYSQL", IntegrationConnection.Type.MYSQL);
        assertEquals("LLM", IntegrationConnection.Type.LLM);
        assertEquals("REDIS", IntegrationConnection.Type.REDIS);
    }

    /**
     * 测试分类常量值
     * <p>
     * 验证 IntegrationConnection.Category 枚举类的常量定义正确。
     *
     * <h3>分类说明：</h3>
     * <ul>
     *   <li>IM: 即时通讯类（Feishu、Slack等）</li>
     *   <li>DATABASE: 数据库类（MySQL、PostgreSQL等）</li>
     *   <li>AI: 人工智能类（LLM、OCR等）</li>
     *   <li>CACHE: 缓存类（Redis、Memcached等）</li>
     * </ul>
     *
     * <h3>用途：</h3>
     * <p>
     * 分类用于对连接进行分组，方便管理和筛选。
     * 例如：在工作流节点选择连接时，可以按分类过滤。
     */
    @Test
    @DisplayName("测试分类常量")
    void testCategoryConstants() {
        assertEquals("IM", IntegrationConnection.Category.IM);
        assertEquals("DATABASE", IntegrationConnection.Category.DATABASE);
        assertEquals("AI", IntegrationConnection.Category.AI);
        assertEquals("CACHE", IntegrationConnection.Category.CACHE);
    }

    /**
     * 测试默认值
     * <p>
     * 验证使用无参构造函数创建 IntegrationConnection 时，
     * 系统赋予的默认值是否正确。
     *
     * <h3>预期默认值：</h3>
     * <ul>
     *   <li>status: "ACTIVE"（新连接默认为活跃状态）</li>
     *   <li>isDefault: false（非默认连接）</li>
     * </ul>
     *
     * <h3>设计理由：</h3>
     * <p>
     * 大部分新建的连接都是活跃且非默认的，
     * 设置合理的默认值可以减少调用方的工作量。
     */
    @Test
    @DisplayName("测试默认值")
    void testDefaultValues() {
        IntegrationConnection conn = new IntegrationConnection();

        assertEquals("ACTIVE", conn.getStatus());
        assertFalse(conn.getIsDefault());
    }
}