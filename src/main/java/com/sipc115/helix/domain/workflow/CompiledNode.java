package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 编译后的节点类
 * <p>
 * 表示工作流中编译后的节点，包含节点ID、类型、动作和配置信息
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Data
public class CompiledNode implements Serializable {
    /**
     * 节点ID
     */
    private String id;
    
    /**
     * 节点类型
     */
    private DslNodeType type;
    
    /**
     * 节点动作
     */
    private String action;
    
    /**
     * 节点配置
     */
    private Map<String, Object> config = new HashMap<>();
}
