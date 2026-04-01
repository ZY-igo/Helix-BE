/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.start;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.CompileContext;
import com.sipc115.helix.integration.workflow.compiler.NodeCompiler;
import org.springframework.stereotype.Component;

/**
 * 开始节点编译器
 * <p>
 * 负责编译 START 类型的节点。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class StartNodeCompiler implements NodeCompiler {
    
    @Override
    public DslNodeType supportType() {
        return DslNodeType.START;
    }
    
    @Override
    public void validate(DslNodeSpec source, CompileContext context) {
        // 开始节点通常不需要特殊验证
    }
    
    @Override
    public CompiledNode compile(DslNodeSpec source, CompileContext context) {
        CompiledNode node = new CompiledNode();
        node.setId(source.getId());
        node.setType(source.getType());
        node.setConfig(source.getConfig());
        node.setAction("START");
        return node;
    }
}
