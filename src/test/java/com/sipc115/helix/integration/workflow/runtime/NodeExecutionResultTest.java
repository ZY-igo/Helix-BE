package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.ExecutionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点执行结果实体测试
 *
 * <p>
 * NodeExecutionResult 是工作流运行时状态管理的核心数据结构。
 * 每个节点执行完毕后都会生成一个 NodeExecutionResult，
 * 包含执行状态、输出数据和后续流转信息。
 */
class NodeExecutionResultTest {

    /**
     * 测试 NodeExecutionResult 基本功能
     * <p>
     * 验证 NodeExecutionResult 的各个属性都能正确设置和获取：
     * <ul>
     *   <li>status: COMPLETED（执行完成）</li>
     *   <li>output: key-value 形式的结果</li>
     *   <li>branchKey: "true"（条件为真）</li>
     *   <li>nextNodeId: "node-002"（下一个节点）</li>
     * </ul>
     *
     * <h3>使用场景：</h3>
     * <p>
     * 节点执行完成后，工作流引擎根据 NodeExecutionResult 决定：
     * <ol>
     *   <li>当前节点是否成功</li>
     *   <li>输出什么数据给下游节点</li>
     *   <li>下一步应该执行哪个节点</li>
     * </ol>
     */
    @Test
    @DisplayName("测试NodeExecutionResult基本功能")
    void testNodeExecutionResultBasicFunctionality() {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setStatus(ExecutionStatus.COMPLETED);
        result.setOutput(Map.of("key", "value"));
        result.setBranchKey("true");
        result.setNextNodeId("node-002");

        assertEquals(ExecutionStatus.COMPLETED, result.getStatus());
        assertEquals("value", result.getOutput().get("key"));
        assertEquals("true", result.getBranchKey());
        assertEquals("node-002", result.getNextNodeId());
    }

    /**
     * 测试 completed 工厂方法
     * <p>
     * 验证 NodeExecutionResult.completed() 工厂方法能创建表示"执行完成"的结果。
     *
     * <h3>工厂方法说明：</h3>
     * <p>
     * 工厂方法提供便捷的创建方式，自动设置合理的默认值。
     * completed() 方法创建的状态为 COMPLETED 的结果对象。
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>status 字段为 COMPLETED</li>
     *   <li>output 字段为非空 Map（便于后续节点读取）</li>
     * </ul>
     */
    @Test
    @DisplayName("测试completed工厂方法")
    void testCompletedFactoryMethod() {
        NodeExecutionResult result = NodeExecutionResult.completed();

        assertEquals(ExecutionStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getOutput());
    }

    /**
     * 测试 waiting 工厂方法
     * <p>
     * 验证 NodeExecutionResult.waiting() 工厂方法能创建表示"等待信号"的结果。
     *
     * <h3>使用场景：</h3>
     * <p>
     * 当节点需要等待外部信号（如定时器、用户确认、回调通知等）才能继续执行时，
     * 使用 waiting() 方法创建等待状态的结果。
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>status 字段为 WAITING_SIGNAL</li>
     *   <li>output 字段为非空 Map</li>
     * </ul>
     *
     * @see ExecutionStatus
     */
    @Test
    @DisplayName("测试waiting工厂方法")
    void testWaitingFactoryMethod() {
        NodeExecutionResult result = NodeExecutionResult.waiting();

        assertEquals(ExecutionStatus.WAITING_SIGNAL, result.getStatus());
        assertNotNull(result.getOutput());
    }

    /**
     * 测试状态为 COMPLETED 时
     * <p>
     * 验证当节点执行成功完成时的状态设置。
     *
     * <h3>COMPLETED 状态说明：</h3>
     * <p>
     * COMPLETED 表示节点正常执行完毕，可以继续后续流程。
     * 这是最常见的成功状态。
     */
    @Test
    @DisplayName("测试状态为COMPLETED时")
    void testCompletedStatus() {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setStatus(ExecutionStatus.COMPLETED);

        assertEquals(ExecutionStatus.COMPLETED, result.getStatus());
    }

    /**
     * 测试状态为 FAILED 时
     * <p>
     * 验证当节点执行失败时的状态设置和错误信息记录。
     *
     * <h3>FAILED 状态说明：</h3>
     * <p>
     * FAILED 表示节点执行过程中发生错误。
     * 错误信息应记录在 output 的 error 字段中，便于问题排查。
     *
     * <h3>错误处理流程：</h3>
     * <ol>
     *   <li>节点执行失败，设置 status 为 FAILED</li>
     *   <li>将错误信息放入 output</li>
     *   <li>工作流引擎根据配置决定是否继续或停止</li>
     * </ol>
     */
    @Test
    @DisplayName("测试状态为FAILED时")
    void testFailedStatus() {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setStatus(ExecutionStatus.FAILED);
        result.setOutput(Map.of("error", "Something went wrong"));

        assertEquals(ExecutionStatus.FAILED, result.getStatus());
        assertEquals("Something went wrong", result.getOutput().get("error"));
    }

    /**
     * 测试设置和获取输出
     * <p>
     * 验证 NodeExecutionResult 的 output 字段能正确存储和返回复杂数据结构。
     *
     * <h3>output 的典型内容：</h3>
     * <ul>
     *   <li>key1: 字符串类型的结果值</li>
     *   <li>key2: 数字类型的结果值</li>
     *   <li>... 其他任意类型的结果数据</li>
     * </ul>
     *
     * <h3>设计灵活性：</h3>
     * <p>
     * output 是 Map<String, Object> 类型，可以存储任意类型的数据，
     * 满足不同节点类型的结果输出需求。
     */
    @Test
    @DisplayName("测试设置和获取输出")
    void testSetAndGetOutput() {
        NodeExecutionResult result = new NodeExecutionResult();
        Map<String, Object> output = new HashMap<>();
        output.put("key1", "value1");
        output.put("key2", 123);

        result.setOutput(output);

        assertEquals("value1", result.getOutput().get("key1"));
        assertEquals(123, result.getOutput().get("key2"));
    }

    /**
     * 测试默认输出不为 null
     * <p>
     * 验证默认情况下 output 字段不为 null。
     * 这确保了即使节点没有显式设置输出，也不会导致空指针异常。
     *
     * <h3>设计理由：</h3>
     * <p>
     * 工作流引擎经常需要读取 output 数据，
     * 如果 output 可能为 null，则每次读取都需要空值判断。
     * 初始化为空 Map 可以简化代码逻辑。
     */
    @Test
    @DisplayName("测试默认输出不为null")
    void testDefaultOutputNotNull() {
        NodeExecutionResult result = new NodeExecutionResult();

        assertNotNull(result.getOutput());
        assertTrue(result.getOutput().isEmpty());
    }
}