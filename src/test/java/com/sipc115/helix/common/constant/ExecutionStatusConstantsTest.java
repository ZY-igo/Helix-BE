/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 执行状态常量测试类
 * <p>
 * 测试 ExecutionStatusConstants 中定义的所有执行状态常量是否正确。
 * 执行状态用于标识工作流和节点的执行阶段，如运行中、成功、失败等。
 *
 * @author Helix Team
 * @since 2.0.0
 */
class ExecutionStatusConstantsTest {

    /**
     * 测试工作流级别的状态常量
     * <p>
     * 工作流执行有三个基本状态：
     * - RUNNING: 工作流正在执行中
     * - SUCCESS: 工作流执行成功完成
     * - FAILED: 工作流执行失败
     *
     * @see ExecutionStatusConstants#WORKFLOW_RUNNING
     * @see ExecutionStatusConstants#WORKFLOW_SUCCESS
     * @see ExecutionStatusConstants#WORKFLOW_FAILED
     */
    @Test
    void testWorkflowStatusConstants() {
        // 验证工作流运行状态常量值为 "RUNNING"
        assertEquals("RUNNING", ExecutionStatusConstants.WORKFLOW_RUNNING);
        // 验证工作流成功状态常量值为 "SUCCESS"
        assertEquals("SUCCESS", ExecutionStatusConstants.WORKFLOW_SUCCESS);
        // 验证工作流失败状态常量值为 "FAILED"
        assertEquals("FAILED", ExecutionStatusConstants.WORKFLOW_FAILED);
    }

    /**
     * 测试节点级别的状态常量
     * <p>
     * 节点执行有六个基本状态：
     * - RUNNING: 节点正在执行中
     * - SUCCESS: 节点执行成功完成
     * - FAILED: 节点执行失败
     * - SKIPPED: 节点被跳过（如条件不满足）
     * - TIMED_OUT: 节点执行超时
     * - COMPLETED: 节点完成（泛指一切正常结束的状态）
     *
     * @see ExecutionStatusConstants#NODE_RUNNING
     * @see ExecutionStatusConstants#NODE_SUCCESS
     * @see ExecutionStatusConstants#NODE_FAILED
     * @see ExecutionStatusConstants#NODE_SKIPPED
     * @see ExecutionStatusConstants#NODE_TIMED_OUT
     * @see ExecutionStatusConstants#NODE_COMPLETED
     */
    @Test
    void testNodeStatusConstants() {
        // 验证节点运行状态常量
        assertEquals("RUNNING", ExecutionStatusConstants.NODE_RUNNING);
        // 验证节点成功状态常量
        assertEquals("SUCCESS", ExecutionStatusConstants.NODE_SUCCESS);
        // 验证节点失败状态常量
        assertEquals("FAILED", ExecutionStatusConstants.NODE_FAILED);
        // 验证节点跳过状态常量
        assertEquals("SKIPPED", ExecutionStatusConstants.NODE_SKIPPED);
        // 验证节点超时状态常量
        assertEquals("TIMED_OUT", ExecutionStatusConstants.NODE_TIMED_OUT);
        // 验证节点完成状态常量
        assertEquals("COMPLETED", ExecutionStatusConstants.NODE_COMPLETED);
    }

    /**
     * 测试所有状态常量都不为 null
     * <p>
     * 确保常量类中的所有状态常量都已正确定义，而不是 null。
     * 这是防止常量类配置错误的防御性检查。
     */
    @Test
    void testStatusValuesAreNotNull() {
        // 验证工作流状态常量不为 null
        assertNotNull(ExecutionStatusConstants.WORKFLOW_RUNNING);
        assertNotNull(ExecutionStatusConstants.WORKFLOW_SUCCESS);
        assertNotNull(ExecutionStatusConstants.WORKFLOW_FAILED);

        // 验证节点状态常量不为 null
        assertNotNull(ExecutionStatusConstants.NODE_RUNNING);
        assertNotNull(ExecutionStatusConstants.NODE_SUCCESS);
        assertNotNull(ExecutionStatusConstants.NODE_FAILED);
        assertNotNull(ExecutionStatusConstants.NODE_SKIPPED);
        assertNotNull(ExecutionStatusConstants.NODE_TIMED_OUT);
        assertNotNull(ExecutionStatusConstants.NODE_COMPLETED);
    }

    /**
     * 测试所有状态常量都不为空字符串
     * <p>
     * 确保常量值是有效的非空字符串，而不是空字符串。
     * 空字符串可能导致数据库查询或比较逻辑出错。
     */
    @Test
    void testStatusValuesAreNotEmpty() {
        // 验证工作流状态常量不为空字符串
        assertFalse(ExecutionStatusConstants.WORKFLOW_RUNNING.isEmpty());
        assertFalse(ExecutionStatusConstants.WORKFLOW_SUCCESS.isEmpty());
        assertFalse(ExecutionStatusConstants.WORKFLOW_FAILED.isEmpty());
    }
}