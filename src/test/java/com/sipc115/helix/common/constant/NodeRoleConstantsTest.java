/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 节点角色常量测试类
 * <p>
 * 测试 NodeRoleConstants 中定义的节点角色常量是否正确。
 * 节点角色用于标识节点在工作流中的位置和作用。
 *
 * @author Helix Team
 * @since 2.0.0
 */
class NodeRoleConstantsTest {

    /**
     * 测试节点角色常量值
     * <p>
     * 节点角色共有三种：
     * - START: 开始节点，工作流的入口点
     * - END: 结束节点，工作流的出口点
     * - NORMAL: 普通节点，位于开始和结束之间的中间节点
     *
     * @see NodeRoleConstants#START
     * @see NodeRoleConstants#END
     * @see NodeRoleConstants#NORMAL
     */
    @Test
    void testNodeRoleConstants() {
        // 验证开始节点角色常量
        assertEquals("START", NodeRoleConstants.START);
        // 验证结束节点角色常量
        assertEquals("END", NodeRoleConstants.END);
        // 验证普通节点角色常量
        assertEquals("NORMAL", NodeRoleConstants.NORMAL);
    }

    /**
     * 测试所有角色常量都不为 null
     * <p>
     * 确保常量类中的所有角色常量都已正确定义。
     */
    @Test
    void testRoleValuesAreNotNull() {
        assertNotNull(NodeRoleConstants.START);
        assertNotNull(NodeRoleConstants.END);
        assertNotNull(NodeRoleConstants.NORMAL);
    }

    /**
     * 测试所有角色常量都不为空字符串
     * <p>
     * 确保常量值是有效的非空字符串。
     */
    @Test
    void testRoleValuesAreNotEmpty() {
        assertFalse(NodeRoleConstants.START.isEmpty());
        assertFalse(NodeRoleConstants.END.isEmpty());
        assertFalse(NodeRoleConstants.NORMAL.isEmpty());
    }
}