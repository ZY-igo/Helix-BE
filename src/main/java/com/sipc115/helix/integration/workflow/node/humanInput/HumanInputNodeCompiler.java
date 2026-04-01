/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.humanInput;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.CompileContext;
import com.sipc115.helix.integration.workflow.compiler.NodeCompiler;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 人工输入节点编译器
 * <p>
 * 负责编译 HUMAN_INPUT 类型的节点。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class HumanInputNodeCompiler implements NodeCompiler {
    
    @Override
    public DslNodeType supportType() {
        return DslNodeType.HUMAN_INPUT;
    }
    
    @Override
    public void validate(DslNodeSpec source, CompileContext context) {
        Map<String, Object> config = source.getConfig();
        if (config == null) {
            throw new IllegalArgumentException("Human input node must have config: " + source.getId());
        }
        
        // 检查表单配置是否存在
        Object form = config.get("form");
        if (form == null) {
            throw new IllegalArgumentException("Human input node must have form config: " + source.getId());
        }
        
        // 检查表单是否为 Map
        if (!(form instanceof Map)) {
            throw new IllegalArgumentException("Form must be a map: " + source.getId());
        }
    }
    
    @Override
    public CompiledNode compile(DslNodeSpec source, CompileContext context) {
        CompiledNode node = new CompiledNode();
        node.setId(source.getId());
        node.setType(source.getType());
        node.setConfig(source.getConfig());
        node.setAction("HUMAN_INPUT");
        return node;
    }
}
