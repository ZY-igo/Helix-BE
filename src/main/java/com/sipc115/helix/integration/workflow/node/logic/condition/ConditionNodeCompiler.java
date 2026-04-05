/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.logic.condition;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.CompileContext;
import com.sipc115.helix.integration.workflow.compiler.NodeCompiler;
import org.springframework.stereotype.Component;

/**
 * 条件节点编译器
 * <p>
 * 负责编译 CONDITION 类型的节点。
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class ConditionNodeCompiler implements NodeCompiler {

    @Override
    public DslNodeType supportType() {
        return DslNodeType.CONDITION;
    }

    @Override
    public void validate(DslNodeSpec source, CompileContext context) {
        if (source.getConfig() == null) {
            throw new IllegalArgumentException("Condition node config cannot be null");
        }
        if (!source.getConfig().containsKey("expression")) {
            throw new IllegalArgumentException("Condition node must have 'expression' config");
        }
    }

    @Override
    public CompiledNode compile(DslNodeSpec source, CompileContext context) {
        CompiledNode node = new CompiledNode();
        node.setId(source.getId());
        node.setType(source.getType());
        node.setConfig(source.getConfig());
        node.setAction("CONDITION");
        return node;
    }
}