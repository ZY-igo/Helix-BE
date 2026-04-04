package com.sipc115.helix.integration.workflow.compiler;

import com.sipc115.helix.domain.workflow.*;
import com.sipc115.helix.integration.workflow.runtime.TransitionResolver;
import com.sipc115.helix.integration.workflow.spi.DslCompiler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DSL编译器组件级集成测试
 *
 * <p>
 * 本测试类验证 DSL 编译器的核心功能：
 * <ol>
 *   <li>DSL 验证：确保工作流定义符合规范</li>
 *   <li>节点编译：各种类型节点能正确编译</li>
 *   <li>转换解析：TransitionResolver 能正确解析流转路径</li>
 * </ol>
 *
 * <h3>测试策略：</h3>
 * <p>
 * 使用 Spring Boot 测试上下文，注入真实的 DslCompiler 和 TransitionResolver 实例，
 * 测试完整的编译流程而非单元测试的模拟对象。
 */
@SpringBootTest
@ActiveProfiles("test")
class DslCompilerComponentTest {

    @Autowired
    private DslCompiler dslCompiler;

    @Autowired
    private TransitionResolver transitionResolver;

    /**
     * 测试编译简单顺序工作流
     * <p>
     * 验证最简单的两节点顺序流程：START -> END
     *
     * <h3>测试的工作流：</h3>
     * <pre>
     * [START] --> [END]
     * </pre>
     *
     * <h3>预期结果：</h3>
     * <ul>
     *   <li>编译成功，返回非空 ExecutionPlan</li>
     *   <li>workflowId 正确传递</li>
     *   <li>节点数量为 2</li>
     *   <li>转换数量为 1</li>
     *   <li>入口节点为 "start"</li>
     * </ul>
     */
    @Test
    @DisplayName("完整工作流编译测试 - 简单顺序流程")
    void testCompileSimpleSequentialWorkflow() {
        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("simple-workflow");
        dsl.setVersion("1.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec startNode = new DslNodeSpec();
        startNode.setId("start");
        startNode.setType(DslNodeType.START);
        startNode.setConfig(new HashMap<>());
        nodes.add(startNode);

        DslNodeSpec middleNode = new DslNodeSpec();
        middleNode.setId("process");
        middleNode.setType(DslNodeType.END);
        middleNode.setConfig(Map.of("resultStatus", "PROCESSED"));
        nodes.add(middleNode);

        dsl.setNodes(nodes);

        List<DslEdgeSpec> edges = new ArrayList<>();

        DslEdgeSpec edge1 = new DslEdgeSpec();
        edge1.setFrom("start");
        edge1.setTo("process");
        edges.add(edge1);

        dsl.setEdges(edges);

        ExecutionPlan plan = dslCompiler.compile(dsl);

        assertNotNull(plan);
        assertEquals("simple-workflow", plan.getWorkflowId());
        assertEquals(2, plan.getNodes().size());
        assertEquals(1, plan.getTransitions().size());
        assertEquals("start", plan.getEntryNodeId());

        String nextNode = transitionResolver.nextNode(plan, "start", null);
        assertEquals("process", nextNode);
    }

    /**
     * 测试编译包含飞书发送节点的工作流
     * <p>
     * 验证飞书消息发送节点的编译正确性。
     *
     * <h3>测试的工作流：</h3>
     * <pre>
     * [START] --> [FEISHU_SEND_TEXT] --> [END]
     * </pre>
     *
     * <h3>飞书节点配置：</h3>
     * <ul>
     *   <li>connectionId: 1L（关联的飞书连接）</li>
     *   <li>chatId: oc_test123（目标群或用户）</li>
     *   <li>text: Hello from workflow（消息内容）</li>
     * </ul>
     *
     * <h3>预期结果：</h3>
     * <ul>
     *   <li>编译成功，节点数量为 3</li>
     *   <li>飞书节点配置正确保留</li>
     *   <li>节点类型为 FEISHU_SEND_TEXT</li>
     * </ul>
     */
    @Test
    @DisplayName("完整工作流编译测试 - 飞书发送节点")
    void testCompileFeishuSendWorkflow() {
        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("feishu-workflow");
        dsl.setVersion("1.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec startNode = new DslNodeSpec();
        startNode.setId("start");
        startNode.setType(DslNodeType.START);
        startNode.setConfig(new HashMap<>());
        nodes.add(startNode);

        DslNodeSpec feishuNode = new DslNodeSpec();
        feishuNode.setId("send-notify");
        feishuNode.setType(DslNodeType.FEISHU_SEND_TEXT);
        feishuNode.setConfig(new HashMap<String, Object>() {{
            put("connectionId", 1L);
            put("chatId", "oc_test123");
            put("text", "Hello from workflow");
        }});
        nodes.add(feishuNode);

        DslNodeSpec endNode = new DslNodeSpec();
        endNode.setId("end");
        endNode.setType(DslNodeType.END);
        endNode.setConfig(new HashMap<>());
        nodes.add(endNode);

        dsl.setNodes(nodes);

        List<DslEdgeSpec> edges = new ArrayList<>();

        DslEdgeSpec edge1 = new DslEdgeSpec();
        edge1.setFrom("start");
        edge1.setTo("send-notify");
        edges.add(edge1);

        DslEdgeSpec edge2 = new DslEdgeSpec();
        edge2.setFrom("send-notify");
        edge2.setTo("end");
        edges.add(edge2);

        dsl.setEdges(edges);

        ExecutionPlan plan = dslCompiler.compile(dsl);

        assertNotNull(plan);
        assertEquals(3, plan.getNodes().size());

        CompiledNode feishuCompiledNode = plan.getNodes().get("send-notify");
        assertNotNull(feishuCompiledNode);
        assertEquals(DslNodeType.FEISHU_SEND_TEXT, feishuCompiledNode.getType());
        assertNotNull(feishuCompiledNode.getConfig());
        assertEquals("oc_test123", feishuCompiledNode.getConfig().get("chatId"));
        assertEquals(1L, feishuCompiledNode.getConfig().get("connectionId"));
    }

    /**
     * 测试验证 - 缺少 START 节点应抛出异常
     * <p>
     * 验证编译器能正确检测并拒绝没有 START 节点的工作流。
     *
     * <h3>业务规则：</h3>
     * <p>
     * 每个工作流必须有且仅有一个 START 节点，作为流程的入口点。
     * 缺少 START 节点的工作流无法确定执行起点。
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>抛出 RuntimeException</li>
     * </ul>
     */
    @Test
    @DisplayName("DSL校验测试 - 缺少START节点应抛出异常")
    void testValidationMissingStartNode() {
        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("invalid-workflow");
        dsl.setVersion("1.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec endNode = new DslNodeSpec();
        endNode.setId("end");
        endNode.setType(DslNodeType.END);
        endNode.setConfig(new HashMap<>());
        nodes.add(endNode);

        dsl.setNodes(nodes);
        dsl.setEdges(new ArrayList<>());

        assertThrows(RuntimeException.class, () -> {
            dslCompiler.compile(dsl);
        }, "Should throw exception when START node is missing");
    }

    /**
     * 测试验证 - 缺少 END 节点应抛出异常
     * <p>
     * 验证编译器能正确检测并拒绝没有 END 节点的工作流。
     *
     * <h3>业务规则：</h3>
     * <p>
     * 每个工作流必须有且仅有一个 END 节点，作为流程的正常结束点。
     * 缺少 END 节点的工作流无法确定执行终点。
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>抛出 RuntimeException</li>
     * </ul>
     */
    @Test
    @DisplayName("DSL校验测试 - 缺少END节点应抛出异常")
    void testValidationMissingEndNode() {
        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("invalid-workflow");
        dsl.setVersion("1.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec startNode = new DslNodeSpec();
        startNode.setId("start");
        startNode.setType(DslNodeType.START);
        startNode.setConfig(new HashMap<>());
        nodes.add(startNode);

        dsl.setNodes(nodes);
        dsl.setEdges(new ArrayList<>());

        assertThrows(RuntimeException.class, () -> {
            dslCompiler.compile(dsl);
        }, "Should throw exception when END node is missing");
    }

    /**
     * 测试 StartNode 正确编译
     * <p>
     * 验证 START 节点能被正确编译并包含在 ExecutionPlan 中。
     *
     * <h3>预期结果：</h3>
     * <ul>
     *   <li>plan 不为 null</li>
     *   <li>入口节点 ID 为 "start"</li>
     *   <li>节点集合包含 "start" 键</li>
     *   <li>start 节点类型为 START</li>
     * </ul>
     */
    @Test
    @DisplayName("节点编译测试 - StartNode正确编译")
    void testStartNodeCompilation() {
        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("start-test");
        dsl.setVersion("1.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec startNode = new DslNodeSpec();
        startNode.setId("start");
        startNode.setType(DslNodeType.START);
        startNode.setConfig(new HashMap<>());
        nodes.add(startNode);

        dsl.setNodes(nodes);
        dsl.setEdges(new ArrayList<>());

        ExecutionPlan plan = dslCompiler.compile(dsl);

        assertNotNull(plan);
        assertEquals("start", plan.getEntryNodeId());
        assertTrue(plan.getNodes().containsKey("start"));

        CompiledNode compiledStart = plan.getNodes().get("start");
        assertEquals(DslNodeType.START, compiledStart.getType());
    }

    /**
     * 测试 EndNode 正确编译
     * <p>
     * 验证 END 节点能被正确编译并包含在 ExecutionPlan 中。
     *
     * <h3>测试的工作流：</h3>
     * <pre>
     * [START] --> [END]
     * </pre>
     *
     * <h3>预期结果：</h3>
     * <ul>
     *   <li>plan 不为 null</li>
     *   <li>节点集合包含 "end" 键</li>
     *   <li>end 节点类型为 END</li>
     * </ul>
     */
    @Test
    @DisplayName("节点编译测试 - EndNode正确编译")
    void testEndNodeCompilation() {
        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("end-test");
        dsl.setVersion("1.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec startNode = new DslNodeSpec();
        startNode.setId("start");
        startNode.setType(DslNodeType.START);
        startNode.setConfig(new HashMap<>());
        nodes.add(startNode);

        DslNodeSpec endNode = new DslNodeSpec();
        endNode.setId("end");
        endNode.setType(DslNodeType.END);
        endNode.setConfig(Map.of("resultStatus", "COMPLETED"));
        nodes.add(endNode);

        dsl.setNodes(nodes);

        List<DslEdgeSpec> edges = new ArrayList<>();
        DslEdgeSpec edge = new DslEdgeSpec();
        edge.setFrom("start");
        edge.setTo("end");
        edges.add(edge);

        dsl.setEdges(edges);

        ExecutionPlan plan = dslCompiler.compile(dsl);

        assertNotNull(plan);
        assertTrue(plan.getNodes().containsKey("end"));

        CompiledNode compiledEnd = plan.getNodes().get("end");
        assertEquals(DslNodeType.END, compiledEnd.getType());
    }

    /**
     * 测试单节点工作流（START = END）
     * <p>
     * 验证只有一个 START 节点（同时也是 END）的工作流能正确编译。
     * 这是最简单的极端情况。
     *
     * <h3>边界情况：</h3>
     * <p>
     * 虽然一个节点同时作为 START 和 END 在实际业务中不常见，
     * 但编译器应该能正确处理这种情况。
     *
     * <h3>预期结果：</h3>
     * <ul>
     *   <li>编译成功</li>
     *   <li>节点数量为 1</li>
     * </ul>
     */
    @Test
    @DisplayName("边界测试 - 单节点工作流(START=END)")
    void testSingleNodeWorkflow() {
        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("single-node");
        dsl.setVersion("1.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec singleNode = new DslNodeSpec();
        singleNode.setId("start");
        singleNode.setType(DslNodeType.START);
        singleNode.setConfig(new HashMap<>());
        nodes.add(singleNode);

        dsl.setNodes(nodes);
        dsl.setEdges(new ArrayList<>());

        ExecutionPlan plan = dslCompiler.compile(dsl);

        assertNotNull(plan);
        assertEquals(1, plan.getNodes().size());
    }

    /**
     * 测试编译多节点顺序工作流
     * <p>
     * 验证包含四个节点的顺序流程编译正确性。
     *
     * <h3>测试的工作流：</h3>
     * <pre>
     * [START] --> [node1] --> [node2] --> [END]
     * </pre>
     *
     * <h3>预期结果：</h3>
     * <ul>
     *   <li>编译成功</li>
     *   <li>节点数量为 4</li>
     *   <li>转换数量为 3</li>
     *   <li>每个转换的下一个节点都正确</li>
     * </ul>
     */
    @Test
    @DisplayName("完整工作流编译测试 - 多节点顺序流程")
    void testCompileMultiNodeSequentialWorkflow() {
        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("multi-node-workflow");
        dsl.setVersion("1.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec startNode = new DslNodeSpec();
        startNode.setId("start");
        startNode.setType(DslNodeType.START);
        startNode.setConfig(new HashMap<>());
        nodes.add(startNode);

        DslNodeSpec node1 = new DslNodeSpec();
        node1.setId("node1");
        node1.setType(DslNodeType.END);
        node1.setConfig(Map.of("resultStatus", "STEP1_DONE"));
        nodes.add(node1);

        DslNodeSpec node2 = new DslNodeSpec();
        node2.setId("node2");
        node2.setType(DslNodeType.END);
        node2.setConfig(Map.of("resultStatus", "STEP2_DONE"));
        nodes.add(node2);

        DslNodeSpec endNode = new DslNodeSpec();
        endNode.setId("end");
        endNode.setType(DslNodeType.END);
        endNode.setConfig(Map.of("resultStatus", "COMPLETED"));
        nodes.add(endNode);

        dsl.setNodes(nodes);

        List<DslEdgeSpec> edges = new ArrayList<>();

        DslEdgeSpec edge1 = new DslEdgeSpec();
        edge1.setFrom("start");
        edge1.setTo("node1");
        edges.add(edge1);

        DslEdgeSpec edge2 = new DslEdgeSpec();
        edge2.setFrom("node1");
        edge2.setTo("node2");
        edges.add(edge2);

        DslEdgeSpec edge3 = new DslEdgeSpec();
        edge3.setFrom("node2");
        edge3.setTo("end");
        edges.add(edge3);

        dsl.setEdges(edges);

        ExecutionPlan plan = dslCompiler.compile(dsl);

        assertNotNull(plan);
        assertEquals(4, plan.getNodes().size());
        assertEquals(3, plan.getTransitions().size());

        String nextFromStart = transitionResolver.nextNode(plan, "start", null);
        assertEquals("node1", nextFromStart);

        String nextFromNode1 = transitionResolver.nextNode(plan, "node1", null);
        assertEquals("node2", nextFromNode1);

        String nextFromNode2 = transitionResolver.nextNode(plan, "node2", null);
        assertEquals("end", nextFromNode2);
    }
}