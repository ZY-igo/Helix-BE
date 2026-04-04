package com.sipc115.helix.integration.workflow.node.logic.loop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

/**
 * LoopResult 循环结果实体测试
 *
 * <p>
 * LoopResult 是循环节点的输出数据结构，
 * 记录了循环执行过程中的关键信息和最终状态。
 */
class LoopResultTest {

    /**
     * 测试 LoopResult 基本功能
     * <p>
     * 验证 LoopResult 的各个属性都能正确设置和获取：
     * <ul>
     *   <li>iterations: 5 次迭代</li>
     *   <li>lastResult: "Final result"</li>
     *   <li>exitReason: "MAX_ROUNDS"（达到最大迭代次数）</li>
     *   <li>success: true（成功完成）</li>
     * </ul>
     *
     * <h3>退出原因说明：</h3>
     * <ul>
     *   <li>MAX_ROUNDS: 达到配置的最大迭代次数</li>
     *   <li>EXIT_CONDITION: 满足退出条件</li>
     *   <li>ERROR: 执行过程中发生错误</li>
     *   <li>COMPLETED: 正常完成</li>
     * </ul>
     */
    @Test
    @DisplayName("测试LoopResult基本功能")
    void testLoopResultBasicFunctionality() {
        LoopResult result = new LoopResult();
        result.setIterations(5);
        result.setLastResult("Final result");
        result.setExitReason("MAX_ROUNDS");
        result.setSuccess(true);

        assertEquals(5, result.getIterations());
        assertEquals("Final result", result.getLastResult());
        assertEquals("MAX_ROUNDS", result.getExitReason());
        assertTrue(result.isSuccess());
    }

    /**
     * 测试 toOutput 方法
     * <p>
     * 验证 LoopResult 转换为输出 Map 的功能。
     * toOutput 方法将结果转换为标准格式，供后续节点使用。
     *
     * <h3>输出格式：</h3>
     * <ul>
     *   <li>iterations: 迭代次数（Integer）</li>
     *   <li>lastResult: 最后结果（String）</li>
     *   <li>exitReason: 退出原因（String）</li>
     *   <li>success: 是否成功（Boolean）</li>
     * </ul>
     */
    @Test
    @DisplayName("测试toOutput方法")
    void testToOutput() {
        LoopResult result = new LoopResult(3, "result data", "EXIT_CONDITION", true);
        Map<String, Object> output = result.toOutput();

        assertEquals(3, output.get("iterations"));
        assertEquals("result data", output.get("lastResult"));
        assertEquals("EXIT_CONDITION", output.get("exitReason"));
        assertEquals(true, output.get("success"));
    }

    /**
     * 测试 toOutput 处理 null lastResult
     * <p>
     * 验证当 lastResult 为 null 时，toOutput 方法能正确处理。
     * null 值应转换为字符串 "null"。
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>lastResult 为 null 时，输出 Map 中对应 "null" 字符串</li>
     * </ul>
     *
     * <h3>设计理由：</h3>
     * <p>
     * Map 不能存储 null 值，转换为 "null" 字符串可以保持输出格式一致性。
     */
    @Test
    @DisplayName("测试toOutput处理null lastResult")
    void testToOutputWithNullLastResult() {
        LoopResult result = new LoopResult(0, null, "ERROR", false);
        Map<String, Object> output = result.toOutput();

        assertEquals("null", output.get("lastResult"));
    }

    /**
     * 测试全参构造函数
     * <p>
     * 验证使用全参构造函数创建 LoopResult 时，
     * 所有参数都能正确赋值。
     *
     * <h3>构造函数参数：</h3>
     * <ol>
     *   <li>iterations: 迭代次数</li>
     *   <li>lastResult: 最后结果（可以是复杂对象）</li>
     *   <li>exitReason: 退出原因</li>
     *   <li>success: 是否成功</li>
     * </ol>
     *
     * <h3>测试数据：</h3>
     * <ul>
     *   <li>iterations: 10</li>
     *   <li>lastResult: Map（复杂对象）</li>
     *   <li>exitReason: "COMPLETED"</li>
     *   <li>success: true</li>
     * </ul>
     */
    @Test
    @DisplayName("测试全参构造函数")
    void testAllArgsConstructor() {
        LoopResult result = new LoopResult(10, Map.of("key", "value"), "COMPLETED", true);

        assertEquals(10, result.getIterations());
        assertNotNull(result.getLastResult());
        assertEquals("COMPLETED", result.getExitReason());
        assertTrue(result.isSuccess());
    }

    /**
     * 测试无参构造函数
     * <p>
     * 验证使用无参构造函数创建 LoopResult 时，
     * 系统赋予的默认值是否正确。
     *
     * <h3>预期默认值：</h3>
     * <ul>
     *   <li>iterations: 0</li>
     *   <li>lastResult: null</li>
     *   <li>exitReason: null</li>
     *   <li>success: false</li>
     * </ul>
     */
    @Test
    @DisplayName("测试无参构造函数")
    void testNoArgsConstructor() {
        LoopResult result = new LoopResult();

        assertEquals(0, result.getIterations());
        assertNull(result.getLastResult());
        assertNull(result.getExitReason());
        assertFalse(result.isSuccess());
    }
}