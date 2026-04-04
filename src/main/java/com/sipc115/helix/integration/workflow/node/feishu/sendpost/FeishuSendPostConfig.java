/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpost;

import lombok.Data;
import java.util.List;

/**
 * 飞书发送富文本消息节点配置
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class FeishuSendPostConfig {

    private String chatId;

    private String title;

    private List<String> lines;
}
