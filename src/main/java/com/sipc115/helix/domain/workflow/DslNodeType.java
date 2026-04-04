/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

/**
 * DSL 节点类型枚举
 * <p>
 * 定义工作流 DSL 支持的所有节点类型。
 *
 * <h3>节点类型说明：</h3>
 * <ul>
 *   <li>START - 开始节点，工作流入口</li>
 *   <li>END - 结束节点，工作流出口</li>
 *   <li>CONDITION - 条件节点，根据条件选择分支</li>
 *   <li>LOOP - 循环节点，重复执行子工作流</li>
 *   <li>HUMAN_INPUT - 人工输入节点，等待用户输入</li>
 *   <li>AI_TASK - AI 任务节点，执行 AI 对话</li>
 *   <li>FEISHU_SEND_TEXT - 飞书发送文本消息</li>
 *   <li>FEISHU_SEND_POST - 飞书发送富文本消息</li>
 *   <li>FEISHU_SEND_POST_WITH_LINK - 飞书发送带链接的富文本消息</li>
 *   <li>FEISHU_PUBLISH_CLOUD_DOC - 飞书发布云文档</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
public enum DslNodeType {
    START("start"),
    END("end"),
    CONDITION("condition"),
    LOOP("loop"),
    HUMAN_INPUT("human_input"),
    AI_TASK("ai_task"),

    FEISHU_SEND_TEXT("feishu_send_text"),
    FEISHU_SEND_POST("feishu_send_post"),
    FEISHU_SEND_POST_WITH_LINK("feishu_send_post_with_link"),
    FEISHU_PUBLISH_CLOUD_DOC("feishu_publish_cloud_doc");

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
