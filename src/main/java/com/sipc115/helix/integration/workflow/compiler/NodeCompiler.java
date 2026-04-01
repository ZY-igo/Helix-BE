/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.compiler;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;

/**
 * 节点编译器接口
 * <p>
 * 每种节点类型的编译器实现此接口，负责编译特定类型的节点。
 * 实现类负责：
 * 1. 验证节点配置的合法性
 * 2. 编译节点配置为 CompiledNode
 * 3. 处理节点特有的编译逻辑
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public interface NodeCompiler {
    
    /**
     * 获取支持的节点类型
     * <p>
     * 返回此编译器支持的节点类型枚举。
     * 
     * @return 支持的节点类型枚举
     */
    DslNodeType supportType();
    
    /**
     * 验证节点配置
     * <p>
     * 验证节点配置的合法性，确保配置符合节点类型的要求。
     * 
     * @param source DSL 节点规范
     * @param context 编译上下文
     * @throws IllegalArgumentException 当节点配置不合法时抛出
     */
    void validate(DslNodeSpec source, CompileContext context);
    
    /**
     * 编译节点
     * <p>
     * 将 DSL 节点规范编译为编译后的节点。
     * 
     * @param source DSL 节点规范
     * @param context 编译上下文
     * @return 编译后的节点
     * @throws IllegalArgumentException 当节点配置不合法时抛出
     */
    CompiledNode compile(DslNodeSpec source, CompileContext context);
}
