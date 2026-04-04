/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 分支键常量测试类
 * <p>
 * 测试 BranchKeyConstants 中定义的条件分支键常量是否正确。
 * 分支键用于条件节点的路由选择，通常为 "true" 或 "false"。
 *
 * @author Helix Team
 * @since 2.0.0
 */
class BranchKeyConstantsTest {

    /**
     * 测试分支键常量值
     * <p>
     * 分支键用于条件节点的路径选择：
     * - TRUE: 条件为真时选择的分支
     * - FALSE: 条件为假时选择的分支
     *
     * @see BranchKeyConstants#TRUE
     * @see BranchKeyConstants#FALSE
     */
    @Test
    void testBranchKeyConstants() {
        // 验证真分支键常量
        assertEquals("true", BranchKeyConstants.TRUE);
        // 验证假分支键常量
        assertEquals("false", BranchKeyConstants.FALSE);
    }

    /**
     * 测试分支键常量不为 null
     */
    @Test
    void testBranchKeyValuesAreNotNull() {
        assertNotNull(BranchKeyConstants.TRUE);
        assertNotNull(BranchKeyConstants.FALSE);
    }

    /**
     * 测试分支键常量值与 Boolean.toString() 结果一致
     * <p>
     * 确保分支键的值可以直接用于布尔值比较。
     */
    @Test
    void testBranchKeyValuesMatchBooleanString() {
        // 验证 TRUE 常量与 Boolean.toString(true) 一致
        assertEquals(String.valueOf(true), BranchKeyConstants.TRUE);
        // 验证 FALSE 常量与 Boolean.toString(false) 一致
        assertEquals(String.valueOf(false), BranchKeyConstants.FALSE);
    }
}