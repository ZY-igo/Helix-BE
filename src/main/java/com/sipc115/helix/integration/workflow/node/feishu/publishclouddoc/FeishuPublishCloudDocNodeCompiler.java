/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.publishclouddoc;

import com.sipc115.helix.domain.integration.IntegrationConnection;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.CompileContext;
import com.sipc115.helix.integration.workflow.compiler.NodeCompiler;
import com.sipc115.helix.repository.jpa.IntegrationConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 飞书发布云文档节点编译器
 *
 * <h3>验证规则：</h3>
 * <ul>
 *   <li>connectionId 必须存在且类型为 FEISHU</li>
 *   <li>title 不能为空</li>
 *   <li>content 不能为空</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuPublishCloudDocNodeCompiler implements NodeCompiler {

    @Autowired
    private IntegrationConnectionRepository connectionRepository;

    @Override
    public DslNodeType supportType() {
        return DslNodeType.FEISHU_PUBLISH_CLOUD_DOC;
    }

    @Override
    public void validate(DslNodeSpec source, CompileContext context) {
        if (source == null) {
            throw new IllegalArgumentException("节点规范不能为空");
        }

        FeishuPublishCloudDocConfig config = parseConfig(source);

        if (config.getConnectionId() == null) {
            throw new IllegalArgumentException("connectionId 不能为空");
        }

        IntegrationConnection connection = connectionRepository.findById(config.getConnectionId())
                .orElseThrow(() -> new IllegalArgumentException("连接不存在: " + config.getConnectionId()));

        if (!"FEISHU".equals(connection.getType())) {
            throw new IllegalArgumentException("连接类型必须为 FEISHU，实际为: " + connection.getType());
        }

        if (config.getTitle() == null || config.getTitle().isEmpty()) {
            throw new IllegalArgumentException("title 不能为空");
        }

        if (config.getContent() == null || config.getContent().isEmpty()) {
            throw new IllegalArgumentException("content 不能为空");
        }
    }

    @Override
    public CompiledNode compile(DslNodeSpec source, CompileContext context) {
        CompiledNode node = new CompiledNode();
        node.setId(source.getId());
        node.setType(source.getType());
        node.setConfig(source.getConfig());
        return node;
    }

    private FeishuPublishCloudDocConfig parseConfig(DslNodeSpec source) {
        FeishuPublishCloudDocConfig config = new FeishuPublishCloudDocConfig();
        if (source.getConfig() != null) {
            Object connectionId = source.getConfig().get("connectionId");
            Object title = source.getConfig().get("title");
            Object content = source.getConfig().get("content");
            if (connectionId != null) {
                if (connectionId instanceof Number) {
                    config.setConnectionId(((Number) connectionId).longValue());
                } else {
                    config.setConnectionId(Long.parseLong(connectionId.toString()));
                }
            }
            if (title != null) config.setTitle(title.toString());
            if (content != null) config.setContent(content.toString());
        }
        return config;
    }
}