/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendtext;

import lombok.Data;

/**
 * 飞书发送文本消息节点配置
 * <p>
 * 用于配置飞书文本消息发送节点的参数。
 *
 * <h3>DSL 中的配置方式：</h3>
 * <pre>
 * {
 *   "type": "FEISHU_SEND_TEXT",
 *   "config": {
 *     "connectionId": 123,          // 飞书连接ID
 *     "chatId": "oc_xxxx",          // 群聊或个人会话ID
 *     "text": "要发送的消息内容"      // 消息文本
 *   }
 * }
 * </pre>
 *
 * <h3>字段说明：</h3>
 * <ul>
 *   <li>connectionId - 飞书集成连接的数据库ID，必须是 FEISHU 类型</li>
 *   <li>chatId - 接收消息的会话ID（open_id 或 chat_id）</li>
 *   <li>text - 消息内容，支持表达式如 ${node-a.output}</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see FeishuSendTextNodeExecutor
 * @see FeishuSendTextNodeDefinition
 */
@Data
public class FeishuSendTextConfig {

    /**
     * 飞书连接 ID
     * <p>
     * 指向 IntegrationConnection 表中 type='FEISHU' 的记录。
     * 用于获取调用飞书 API 所需的 appId、appSecret 和 tenantAccessToken。
     */
    private Long connectionId;

    /**
     * 飞书会话 ID
     * <p>
     * 支持两种类型：
     * <ul>
     *   <li>chat_id - 群聊会话 ID</li>
     *   <li>open_id - 用户的开放 ID</li>
     * </ul>
     * 飞书文档：https://open.feishu.cn/document/uAjLw4CM/ukTMukTMukTM/reference/im-v1/message/create
     */
    private String chatId;

    /**
     * 消息文本内容
     * <p>
     * 支持以下格式：
     * <ul>
     *   <li>普通文本 - 直接发送</li>
     *   <li>表达式 - ${variable.name} 形式，运行时求值</li>
     *   <li>多行文本 - 使用 \n 分隔</li>
     * </ul>
     */
    private String text;
}