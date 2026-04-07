/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendtext;

import com.sipc115.helix.common.constant.WorkflowConstants;
import com.sipc115.helix.domain.integration.IntegrationConnection;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.connect.ConnectionClientRegistry;
import com.sipc115.helix.integration.workflow.compiler.CompileContext;
import com.sipc115.helix.integration.workflow.compiler.NodeCompiler;
import com.sipc115.helix.repository.jpa.IntegrationConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 飞书发送文本消息节点编译器
 * <p>
 * 负责编译飞书发送文本消息节点（FEISHU_SEND_TEXT）。
 *
 * <h3>验证规则：</h3>
 * <ul>
 *   <li>chatId 不能为空</li>
 *   <li>text 不能为空</li>
 *   <li>connectionId 必须存在且对应的连接类型为 FEISHU</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuSendTextNodeCompiler implements NodeCompiler {

    @Autowired
    private IntegrationConnectionRepository connectionRepository;

    @Autowired
    private ConnectionClientRegistry connectionRegistry;

    @Override
    public DslNodeType supportType() {
        return DslNodeType.FEISHU_SEND_TEXT;
    }

    @Override
    public void validate(DslNodeSpec source, CompileContext context) {
        if (source == null) {
            throw new IllegalArgumentException("节点规范不能为空");
        }

        FeishuSendTextConfig config = parseConfig(source);

        if (config.getChatId() == null || config.getChatId().isEmpty()) {
            throw new IllegalArgumentException("chatId 不能为空");
        }

        if (config.getText() == null || config.getText().isEmpty()) {
            throw new IllegalArgumentException("text 不能为空");
        }

        if (config.getConnectionId() == null) {
            throw new IllegalArgumentException("connectionId 不能为空");
        }

        IntegrationConnection connection = connectionRepository.findById(config.getConnectionId())
                .orElseThrow(() -> new IllegalArgumentException("连接不存在: " + config.getConnectionId()));

        if (!"FEISHU".equals(connection.getType())) {
            throw new IllegalArgumentException("连接类型必须为 FEISHU，实际为: " + connection.getType());
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

    private FeishuSendTextConfig parseConfig(DslNodeSpec source) {
        FeishuSendTextConfig config = new FeishuSendTextConfig();
        if (source.getConfig() != null) {
            Object chatId = source.getConfig().get("chatId");
            Object text = source.getConfig().get("text");
            Object connectionId = source.getConfig().get("connectionId");
            if (chatId != null) config.setChatId(chatId.toString());
            if (text != null) config.setText(text.toString());
            if (connectionId != null) {
                if (connectionId instanceof Number) {
                    config.setConnectionId(((Number) connectionId).longValue());
                } else {
                    config.setConnectionId(Long.parseLong(connectionId.toString()));
                }
            }
        }
        return config;
    }
}