/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流 DSL 节点规范类
 * <p>
 * 用于定义工作流 DSL 中的节点（node）规范，表示工作流中的一个执行步骤。
 * 包含节点 ID、类型、名称和配置信息等，用于构建工作流的执行逻辑。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class DslNodeSpec {
    /**
     * 节点 ID
     * <p>
     * 节点的唯一标识符，用于在工作流中引用此节点。
     */
    private String id;
    
    /**
     * 节点类型
     * <p>
     * 节点的类型，如 START、END、ACTIVITY、HUMAN_INPUT 等，决定了节点的执行逻辑。
     */
    private DslNodeType type;
    
    /**
     * 节点名称
     * <p>
     * 节点的显示名称，用于在界面上展示节点。
     */
    private String name;
    
    /**
     * 节点配置
     * <p>
     * 节点的配置参数，以键值对形式存储。
     * 默认为空 HashMap，确保始终可用。
     */
    private Map<String, Object> config = new HashMap<>();
}
