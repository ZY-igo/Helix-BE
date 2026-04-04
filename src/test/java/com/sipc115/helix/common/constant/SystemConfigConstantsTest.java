/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统配置常量测试类
 * <p>
 * 测试 SystemConfigConstants 中定义的系统级配置常量是否正确。
 * 这些常量用于工作流的默认配置，如超时时间、循环变量名等。
 *
 * @author Helix Team
 * @since 2.0.0
 */
class SystemConfigConstantsTest {

    /**
     * 测试超时配置常量
     * <p>
     * 系统定义了两种超时时间单位：
     * - DEFAULT_TIMEOUT_HOURS: 默认超时小时数（24小时）
     * - DEFAULT_TIMEOUT_SECONDS: 默认超时秒数（3600秒）
     *
     * @see SystemConfigConstants#DEFAULT_TIMEOUT_HOURS
     * @see SystemConfigConstants#DEFAULT_TIMEOUT_SECONDS
     */
    @Test
    void testTimeoutConstants() {
        // 验证默认超时小时数为24小时
        assertEquals(24, SystemConfigConstants.DEFAULT_TIMEOUT_HOURS);
        // 验证默认超时秒数为3600秒（1小时）
        assertEquals(3600, SystemConfigConstants.DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 测试循环节点变量名常量
     * <p>
     * 循环节点使用两个特殊变量：
     * - DEFAULT_LOOP_VARIABLE: 迭代计数器变量名（默认为 "iteration"）
     * - DEFAULT_RESULT_VARIABLE: 循环结果变量名（默认为 "loopResult"）
     *
     * @see SystemConfigConstants#DEFAULT_LOOP_VARIABLE
     * @see SystemConfigConstants#DEFAULT_RESULT_VARIABLE
     */
    @Test
    void testLoopVariableConstants() {
        // 验证迭代变量名
        assertEquals("iteration", SystemConfigConstants.DEFAULT_LOOP_VARIABLE);
        // 验证结果变量名
        assertEquals("loopResult", SystemConfigConstants.DEFAULT_RESULT_VARIABLE);
    }

    /**
     * 测试条件分支默认常量
     * <p>
     * 当条件节点没有匹配的分支时，使用默认分支。
     * 默认为 "true"，即条件为真时走默认分支。
     *
     * @see SystemConfigConstants#DEFAULT_CONDITION_BRANCH
     */
    @Test
    void testConditionBranchConstant() {
        // 验证默认条件分支为 "true"
        assertEquals("true", SystemConfigConstants.DEFAULT_CONDITION_BRANCH);
    }

    /**
     * 测试超时常量值为正数
     * <p>
     * 超时时间必须是正数，否则可能导致工作流立即超时。
     */
    @Test
    void testTimeoutValuesArePositive() {
        // 验证默认超时小时数大于0
        assertTrue(SystemConfigConstants.DEFAULT_TIMEOUT_HOURS > 0);
        // 验证默认超时秒数大于0
        assertTrue(SystemConfigConstants.DEFAULT_TIMEOUT_SECONDS > 0);
    }

    /**
     * 测试变量名常量不为空
     */
    @Test
    void testVariableNamesAreNotEmpty() {
        assertFalse(SystemConfigConstants.DEFAULT_LOOP_VARIABLE.isEmpty());
        assertFalse(SystemConfigConstants.DEFAULT_RESULT_VARIABLE.isEmpty());
    }
}