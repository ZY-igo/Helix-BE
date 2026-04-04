/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpostwithlink;

import lombok.Data;

/**
 * 飞书发送带链接富文本消息节点配置
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class FeishuSendPostWithLinkConfig {

    private String chatId;

    private String title;

    private String text;

    private String url;

    private String linkText;
}
