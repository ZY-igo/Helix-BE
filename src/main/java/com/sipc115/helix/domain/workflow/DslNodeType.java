package com.sipc115.helix.domain.workflow;

public enum DslNodeType {
    START("start"),
    END("end"),
    CONDITION("condition"),
    LOOP("loop"),
    HUMAN_INPUT("human_input"),
    AI_TASK("ai_task"),
    FEISHU_NOTIFICATION("feishu_notification");

    private final String value;

    DslNodeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DslNodeType fromValue(String value) {
        for (DslNodeType type : DslNodeType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown node type: " + value);
    }
}
