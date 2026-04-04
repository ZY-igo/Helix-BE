/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendtext;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.CompileContext;
import com.sipc115.helix.integration.workflow.compiler.NodeCompiler;
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
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuSendTextNodeCompiler implements NodeCompiler {

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
            if (chatId != null) config.setChatId(chatId.toString());
            if (text != null) config.setText(text.toString());
        }
        return config;
    }
}
