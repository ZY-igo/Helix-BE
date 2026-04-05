/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpost;

import lombok.Data;
import java.util.List;

@Data
public class FeishuSendPostConfig {

    private Long connectionId;

    private String chatId;

    private String title;

    private List<String> lines;
}