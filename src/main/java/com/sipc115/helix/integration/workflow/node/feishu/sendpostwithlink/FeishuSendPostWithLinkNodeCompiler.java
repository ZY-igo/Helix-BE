/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpostwithlink;

import com.sipc115.helix.common.constant.WorkflowConstants;
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
 * 飞书发送带链接富文本消息节点编译器
 *
 * <h3>验证规则：</h3>
 * <ul>
 *   <li>connectionId 必须存在且类型为 FEISHU</li>
 *   <li>chatId 不能为空</li>
 *   <li>title 不能为空</li>
 *   <li>text 不能为空</li>
 *   <li>url 不能为空</li>
 *   <li>linkText 不能为空</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuSendPostWithLinkNodeCompiler implements NodeCompiler {

    @Autowired
    private IntegrationConnectionRepository connectionRepository;

    @Override
    public DslNodeType supportType() {
        return DslNodeType.FEISHU_SEND_POST_WITH_LINK;
    }

    @Override
    public void validate(DslNodeSpec source, CompileContext context) {
        if (source == null) {
            throw new IllegalArgumentException("节点规范不能为空");
        }

        FeishuSendPostWithLinkConfig config = parseConfig(source);

        if (config.getConnectionId() == null) {
            throw new IllegalArgumentException("connectionId 不能为空");
        }

        IntegrationConnection connection = connectionRepository.findById(config.getConnectionId())
                .orElseThrow(() -> new IllegalArgumentException("连接不存在: " + config.getConnectionId()));

        if (!"FEISHU".equals(connection.getType())) {
            throw new IllegalArgumentException("连接类型必须为 FEISHU，实际为: " + connection.getType());
        }

        if (config.getChatId() == null || config.getChatId().isEmpty()) {
            throw new IllegalArgumentException("chatId 不能为空");
        }

        if (config.getTitle() == null || config.getTitle().isEmpty()) {
            throw new IllegalArgumentException("title 不能为空");
        }

        if (config.getText() == null || config.getText().isEmpty()) {
            throw new IllegalArgumentException("text 不能为空");
        }

        if (config.getUrl() == null || config.getUrl().isEmpty()) {
            throw new IllegalArgumentException("url 不能为空");
        }

        if (config.getLinkText() == null || config.getLinkText().isEmpty()) {
            throw new IllegalArgumentException("linkText 不能为空");
        }

        if (source.getConfig() != null && connection.getConfig() != null) {
            source.getConfig().put(WorkflowConstants.CONNECTION_CONFIG_KEY, connection.getConfig());
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

    private FeishuSendPostWithLinkConfig parseConfig(DslNodeSpec source) {
        FeishuSendPostWithLinkConfig config = new FeishuSendPostWithLinkConfig();
        if (source.getConfig() != null) {
            Object connectionId = source.getConfig().get("connectionId");
            Object chatId = source.getConfig().get("chatId");
            Object title = source.getConfig().get("title");
            Object text = source.getConfig().get("text");
            Object url = source.getConfig().get("url");
            Object linkText = source.getConfig().get("linkText");
            if (connectionId != null) {
                if (connectionId instanceof Number) {
                    config.setConnectionId(((Number) connectionId).longValue());
                } else {
                    config.setConnectionId(Long.parseLong(connectionId.toString()));
                }
            }
            if (chatId != null) config.setChatId(chatId.toString());
            if (title != null) config.setTitle(title.toString());
            if (text != null) config.setText(text.toString());
            if (url != null) config.setUrl(url.toString());
            if (linkText != null) config.setLinkText(linkText.toString());
        }
        return config;
    }
}