/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpostwithlink;

import lombok.Data;

@Data
public class FeishuSendPostWithLinkConfig {

    private Long connectionId;

    private String chatId;

    private String title;

    private String text;

    private String url;

    private String linkText;
}