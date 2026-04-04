/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.publishclouddoc;

import lombok.Data;

/**
 * 飞书发布云文档节点配置
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class FeishuPublishCloudDocConfig {

    private String title;

    private String content;
}
