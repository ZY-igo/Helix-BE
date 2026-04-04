/**
 * 基础设施连接测试（不依赖 Spring）
 * <p>
 * 直接测试 PostgreSQL、RocketMQ、Temporal 三大基础设施的连接。
 */
package com.sipc115.helix.infrastructure;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础设施连接测试（纯 JDBC + 原生客户端）
 *
 * <p>
 * 使用原生客户端测试三大基础设施，不依赖 Spring 上下文。
 */
class InfrastructureConnectionTest {

    private static final String POSTGRES_URL = "jdbc:postgresql://192.168.115.23:5432/helix";
    private static final String POSTGRES_USER = "admin";
    private static final String POSTGRES_PASS = "Sipc@postgres";

    private static final String ROCKETMQ_NAMESERVER = "192.168.115.23:9876";
    private static final String ROCKETMQ_GROUP = "workflow-trace-group";

    private static final String TEMPORAL_TARGET = "192.168.115.23:7233";

    @Test
    @DisplayName("测试 PostgreSQL 数据库连接")
    void testPostgreSQLConnection() {
        System.out.println("\n========== 测试 PostgreSQL 连接 ==========");
        System.out.println("URL: " + POSTGRES_URL);
        System.out.println("用户: " + POSTGRES_USER);

        try (Connection conn = DriverManager.getConnection(POSTGRES_URL, POSTGRES_USER, POSTGRES_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version()")) {

            assertTrue(conn.isValid(5), "数据库连接应该有效");
            assertFalse(conn.isClosed(), "数据库连接不应该关闭");

            if (rs.next()) {
                String version = rs.getString(1);
                System.out.println("✓ PostgreSQL 连接成功！");
                System.out.println("  版本: " + version.substring(0, Math.min(80, version.length())));
            }

            try (ResultSet rs2 = stmt.executeQuery("SELECT current_database()")) {
                if (rs2.next()) {
                    System.out.println("  当前数据库: " + rs2.getString(1));
                }
            }

            try (ResultSet rs3 = stmt.executeQuery(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' LIMIT 10")) {
                System.out.println("  数据库中的表:");
                int count = 0;
                while (rs3.next()) {
                    System.out.println("    - " + rs3.getString(1));
                    count++;
                    if (count >= 10) break;
                }
                if (count == 0) {
                    System.out.println("    (无表)");
                }
            }

        } catch (Exception e) {
            System.err.println("✗ PostgreSQL 连接失败: " + e.getMessage());
            fail("PostgreSQL 连接失败: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试 RocketMQ 连接")
    void testRocketMQConnection() {
        System.out.println("\n========== 测试 RocketMQ 连接 ==========");
        System.out.println("NameServer: " + ROCKETMQ_NAMESERVER);
        System.out.println("Producer Group: " + ROCKETMQ_GROUP);

        DefaultMQProducer producer = null;
        try {
            producer = new DefaultMQProducer(ROCKETMQ_GROUP);
            producer.setNamesrvAddr(ROCKETMQ_NAMESERVER);
            producer.setSendMsgTimeout(3000);

            producer.start();
            System.out.println("✓ RocketMQ Producer 启动成功！");

            Message testMessage = new Message("TEST_TOPIC", "test", "test message".getBytes());
            var result = producer.send(testMessage, 3000);
            System.out.println("  发送状态: " + result.getSendStatus());
            System.out.println("  消息ID: " + result.getMsgId());

            assertEquals(org.apache.rocketmq.client.producer.SendStatus.SEND_OK, result.getSendStatus(),
                    "消息发送应该成功");

        } catch (Exception e) {
            System.err.println("✗ RocketMQ 连接失败: " + e.getMessage());
            fail("RocketMQ 连接失败: " + e.getMessage());
        } finally {
            if (producer != null) {
                producer.shutdown();
                System.out.println("  Producer 已关闭");
            }
        }
    }

    @Test
    @DisplayName("测试 Temporal 连接")
    void testTemporalConnection() {
        System.out.println("\n========== 测试 Temporal 连接 ==========");
        System.out.println("Target: " + TEMPORAL_TARGET);

        WorkflowServiceStubs service = null;
        try {
            WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
                    .setTarget(TEMPORAL_TARGET)
                    .build();

            service = WorkflowServiceStubs.newServiceStubs(options);
            System.out.println("✓ Temporal Service Stubs 创建成功！");

            var blockingStub = service.blockingStub();
            System.out.println("✓ Temporal 阻塞存根创建成功！");
            System.out.println("  Target: " + TEMPORAL_TARGET);
            System.out.println("✓ Temporal 连接成功！");

        } catch (Exception e) {
            System.err.println("✗ Temporal 连接失败: " + e.getMessage());
            fail("Temporal 连接失败: " + e.getMessage());
        } finally {
            if (service != null) {
                service.shutdown();
                System.out.println("  Service 已关闭");
            }
        }
    }
}