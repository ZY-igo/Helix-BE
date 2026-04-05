/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.publishclouddoc;

import lombok.Data;

@Data
public class FeishuPublishCloudDocConfig {

    private Long connectionId;

    private String title;

    private String content;
}