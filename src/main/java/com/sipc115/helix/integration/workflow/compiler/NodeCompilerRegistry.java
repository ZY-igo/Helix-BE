/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.compiler;

import com.sipc115.helix.domain.workflow.DslNodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;

/**
 * 节点编译器注册中心
 * <p>
 * 负责管理所有节点编译器，根据节点类型获取对应的编译器。
 * 使用 EnumMap 存储编译器，确保 O(1) 查找效率。
 * 支持启动时重复注册检测，实现 Fail Fast 原则。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class NodeCompilerRegistry {
    
    /**
     * 节点编译器映射
     * <p>
     * 键：节点类型枚举
     * 值：对应的节点编译器
     */
    private final EnumMap<DslNodeType, NodeCompiler> compilerMap;
    
    /**
     * 构造函数
     * <p>
     * 自动注入所有 NodeCompiler 实现，并注册到 EnumMap 中。
     * 检测重复注册，实现 Fail Fast 原则。
     * 
     * @param compilers 所有节点编译器实现
     * @throws IllegalStateException 当检测到重复注册时抛出
     */
    @Autowired
    public NodeCompilerRegistry(List<NodeCompiler> compilers) {
        this.compilerMap = new EnumMap<>(DslNodeType.class);
        
        for (NodeCompiler compiler : compilers) {
            DslNodeType type = compiler.supportType();
            
            // 检测重复注册
            if (compilerMap.containsKey(type)) {
                throw new IllegalStateException("Duplicate compiler registration for node type: " + type);
            }
            
            compilerMap.put(type, compiler);
        }
    }
    
    /**
     * 获取节点编译器
     * <p>
     * 根据节点类型枚举获取对应的节点编译器。
     * 
     * @param type 节点类型枚举
     * @return 对应的节点编译器
     * @throws IllegalArgumentException 当未找到对应节点类型的编译器时抛出
     */
    public NodeCompiler getRequiredCompiler(DslNodeType type) {
        NodeCompiler compiler = compilerMap.get(type);
        if (compiler == null) {
            throw new IllegalArgumentException("No compiler found for node type: " + type);
        }
        return compiler;
    }
    
    /**
     * 检查是否存在对应节点类型的编译器
     * <p>
     * 检查是否存在对应节点类型的编译器。
     * 
     * @param type 节点类型枚举
     * @return 是否存在对应的编译器
     */
    public boolean hasCompiler(DslNodeType type) {
        return compilerMap.containsKey(type);
    }
    
    /**
     * 获取所有支持的节点类型
     * <p>
     * 返回所有已注册的节点类型枚举。
     * 
     * @return 支持的节点类型枚举列表
     */
    public List<DslNodeType> getSupportedNodeTypes() {
        return compilerMap.keySet().stream().toList();
    }
    
    /**
     * 获取注册的编译器数量
     * <p>
     * 返回当前注册的编译器数量。
     * 
     * @return 编译器数量
     */
    public int getCompilerCount() {
        return compilerMap.size();
    }
}
