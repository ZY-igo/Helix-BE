package com.sipc115.helix.integration.workflow.node.agent.chat;

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

import java.util.Map;

/**
 * AI 聊天节点编译器
 * <p>
 * 负责编译 AI_TASK 类型的节点，验证配置并生成编译后的节点。
 *
 * <h3>验证规则：</h3>
 * <ul>
 *   <li>connectionId 必须存在且对应的连接类型为 LLM</li>
 *   <li>userPrompt 不能为空</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class AiChatNodeCompiler implements NodeCompiler {

    @Autowired
    private IntegrationConnectionRepository connectionRepository;

    @Override
    public DslNodeType supportType() {
        return DslNodeType.AI_TASK;
    }

    @Override
    public void validate(DslNodeSpec source, CompileContext context) {
        if (source == null) {
            throw new IllegalArgumentException("节点规范不能为空");
        }

        Map<String, Object> config = source.getConfig();
        if (config == null) {
            throw new IllegalArgumentException("AI 聊天节点必须包含配置: " + source.getId());
        }

        Object connectionIdObj = config.get("connectionId");
        if (connectionIdObj == null) {
            throw new IllegalArgumentException("AI 聊天节点必须指定 connectionId: " + source.getId());
        }

        Long connectionId;
        if (connectionIdObj instanceof Number) {
            connectionId = ((Number) connectionIdObj).longValue();
        } else {
            connectionId = Long.parseLong(connectionIdObj.toString());
        }

        IntegrationConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("连接不存在: " + connectionId));

        if (!"LLM".equals(connection.getType())) {
            throw new IllegalArgumentException("连接类型必须为 LLM，实际为: " + connection.getType());
        }

        Object userPrompt = config.get("userPrompt");
        if (userPrompt == null) {
            throw new IllegalArgumentException("AI 聊天节点必须指定 userPrompt: " + source.getId());
        }

        String userPromptStr = userPrompt.toString();
        if (userPromptStr.isEmpty()) {
            throw new IllegalArgumentException("AI 聊天节点的 userPrompt 不能为空: " + source.getId());
        }

        if (connection.getConfig() != null) {
            config.put(WorkflowConstants.CONNECTION_CONFIG_KEY, connection.getConfig());
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
}
