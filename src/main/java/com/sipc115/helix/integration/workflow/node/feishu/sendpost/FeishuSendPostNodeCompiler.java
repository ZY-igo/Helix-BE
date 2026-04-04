/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpost;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.CompileContext;
import com.sipc115.helix.integration.workflow.compiler.NodeCompiler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 飞书发送富文本消息节点编译器
 *
 * <h3>验证规则：</h3>
 * <ul>
 *   <li>chatId 不能为空</li>
 *   <li>title 不能为空</li>
 *   <li>lines 不能为空</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuSendPostNodeCompiler implements NodeCompiler {

    @Override
    public DslNodeType supportType() {
        return DslNodeType.FEISHU_SEND_POST;
    }

    @Override
    public void validate(DslNodeSpec source, CompileContext context) {
        if (source == null) {
            throw new IllegalArgumentException("节点规范不能为空");
        }

        FeishuSendPostConfig config = parseConfig(source);

        if (config.getChatId() == null || config.getChatId().isEmpty()) {
            throw new IllegalArgumentException("chatId 不能为空");
        }

        if (config.getTitle() == null || config.getTitle().isEmpty()) {
            throw new IllegalArgumentException("title 不能为空");
        }

        if (config.getLines() == null || config.getLines().isEmpty()) {
            throw new IllegalArgumentException("lines 不能为空");
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

    @SuppressWarnings("unchecked")
    private FeishuSendPostConfig parseConfig(DslNodeSpec source) {
        FeishuSendPostConfig config = new FeishuSendPostConfig();
        if (source.getConfig() != null) {
            Object chatId = source.getConfig().get("chatId");
            Object title = source.getConfig().get("title");
            Object lines = source.getConfig().get("lines");
            if (chatId != null) config.setChatId(chatId.toString());
            if (title != null) config.setTitle(title.toString());
            if (lines instanceof List) config.setLines((List<String>) lines);
        }
        return config;
    }
}
