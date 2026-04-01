package com.sipc115.helix.domain.workflow;

/**
 * 节点类型枚举
 * <p>
 * 定义工作流中支持的所有节点类型。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public enum DslNodeType {
    START("start"),
    END("end"),
    ACTIVITY("activity"),
    CONDITION("condition"),
    DELAY("delay"),
    HUMAN_INPUT("human_input"),
    CHILD_WORKFLOW("child_workflow"),
    TRANSFORM("transform"),
    AI_TASK("ai_task");
    
    private final String value;
    
    /**
     * 构造函数
     * 
     * @param value 节点类型的字符串值
     */
    DslNodeType(String value) {
        this.value = value;
    }
    
    /**
     * 获取节点类型的字符串值
     * 
     * @return 节点类型的字符串值
     */
    public String getValue() {
        return value;
    }
    
    /**
     * 根据字符串值获取枚举实例
     * 
     * @param value 节点类型的字符串值
     * @return 对应的枚举实例
     * @throws IllegalArgumentException 当不存在对应的值时抛出
     */
    public static DslNodeType fromValue(String value) {
        for (DslNodeType type : DslNodeType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown node type: " + value);
    }
}
