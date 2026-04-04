/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendtext;

import lombok.Data;

/**
 * 飞书发送文本消息节点配置
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class FeishuSendTextConfig {

    private String chatId;

    private String text;
}
