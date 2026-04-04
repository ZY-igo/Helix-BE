/**
 * DSL编译器纯单元测试
 * <p>
 * 不依赖 Spring 上下文，直接构建编译器并拿真实 DSL 去编译。
 */
package com.sipc115.helix.integration.workflow.compiler;

import com.sipc115.helix.domain.workflow.*;
import com.sipc115.helix.integration.expression.aviator.AviatorExpressionEngine;
import com.sipc115.helix.integration.expression.ExpressionEngine;
import com.sipc115.helix.integration.workflow.node.end.EndNodeCompiler;
import com.sipc115.helix.integration.workflow.node.feishu.sendtext.FeishuSendTextNodeCompiler;
import com.sipc115.helix.integration.workflow.node.start.StartNodeCompiler;
import com.sipc115.helix.integration.workflow.runtime.TransitionResolver;
import com.sipc115.helix.integration.workflow.spi.DslCompiler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DSL编译器纯单元测试
 *
 * <p>
 * 测试策略：
 * - 不启动 Spring 上下文
 * - 手动构建 DefaultDslCompiler 和它的依赖
 * - 用真实的 DSL 对象调用编译方法
 * - 验证编译结果
 */
class DslCompilerPureUnitTest {

    private DefaultDslCompiler compiler;
    private TransitionResolver transitionResolver;

    @BeforeEach
    void setUp() throws Exception {
        compiler = new DefaultDslCompiler();

        ExpressionEngine expressionEngine = new AviatorExpressionEngine();
        NodeCompilerRegistry registry = new NodeCompilerRegistry(List.of(
                new StartNodeCompiler(),
                new EndNodeCompiler(),
                new FeishuSendTextNodeCompiler()
        ));

        setField(compiler, "expressionEngine", expressionEngine);
        setField(compiler, "nodeCompilerRegistry", registry);

        transitionResolver = new TransitionResolver();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("【单元测试】编译最简单的 START -> END 工作流")
    void testSimplestWorkflow() {
        System.out.println("\n========== 测试：最简单的 START -> END 工作流 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("simplest-workflow");
        dsl.setVersion("1.0.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec start = new DslNodeSpec();
        start.setId("start");
        start.setType(DslNodeType.START);
        start.setConfig(new HashMap<>());
        nodes.add(start);

        DslNodeSpec end = new DslNodeSpec();
        end.setId("end");
        end.setType(DslNodeType.END);
        end.setConfig(Map.of("resultStatus", "COMPLETED"));
        nodes.add(end);

        dsl.setNodes(nodes);

        List<DslEdgeSpec> edges = new ArrayList<>();
        DslEdgeSpec edge = new DslEdgeSpec();
        edge.setFrom("start");
        edge.setTo("end");
        edges.add(edge);
        dsl.setEdges(edges);

        System.out.println("DSL: " + dsl.getWorkflowId() + ", 节点数=" + nodes.size() + ", 边数=" + edges.size());

        ExecutionPlan plan = compiler.compile(dsl);

        System.out.println("✓ 编译成功！");
        System.out.println("  - workflowId: " + plan.getWorkflowId());
        System.out.println("  - entryNodeId: " + plan.getEntryNodeId());
        System.out.println("  - 节点数: " + plan.getNodes().size());
        System.out.println("  - 转换数: " + plan.getTransitions().size());

        assertNotNull(plan);
        assertEquals("simplest-workflow", plan.getWorkflowId());
        assertEquals("start", plan.getEntryNodeId());
        assertEquals(2, plan.getNodes().size());
        assertEquals(1, plan.getTransitions().size());
        assertTrue(plan.getNodes().containsKey("start"));
        assertTrue(plan.getNodes().containsKey("end"));
    }

    @Test
    @DisplayName("【单元测试】编译三节点顺序工作流: START -> node1 -> END")
    void testThreeNodeWorkflow() {
        System.out.println("\n========== 测试：三节点顺序工作流 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("three-node-workflow");
        dsl.setVersion("1.0.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec start = new DslNodeSpec();
        start.setId("start");
        start.setType(DslNodeType.START);
        start.setConfig(new HashMap<>());
        nodes.add(start);

        DslNodeSpec middle = new DslNodeSpec();
        middle.setId("node1");
        middle.setType(DslNodeType.END);
        middle.setConfig(Map.of("resultStatus", "STEP1_DONE"));
        nodes.add(middle);

        DslNodeSpec end = new DslNodeSpec();
        end.setId("end");
        end.setType(DslNodeType.END);
        end.setConfig(Map.of("resultStatus", "COMPLETED"));
        nodes.add(end);

        dsl.setNodes(nodes);

        List<DslEdgeSpec> edges = new ArrayList<>();
        DslEdgeSpec edge1 = new DslEdgeSpec();
        edge1.setFrom("start");
        edge1.setTo("node1");
        edges.add(edge1);

        DslEdgeSpec edge2 = new DslEdgeSpec();
        edge2.setFrom("node1");
        edge2.setTo("end");
        edges.add(edge2);

        dsl.setEdges(edges);

        ExecutionPlan plan = compiler.compile(dsl);

        System.out.println("✓ 编译成功！");
        System.out.println("  - workflowId: " + plan.getWorkflowId());
        System.out.println("  - 入口节点: " + plan.getEntryNodeId());
        System.out.println("  - 节点列表: " + plan.getNodes().keySet());

        assertEquals(3, plan.getNodes().size());
        assertEquals(2, plan.getTransitions().size());

        String nextFromStart = transitionResolver.nextNode(plan, "start", null);
        assertEquals("node1", nextFromStart);
        System.out.println("  - start -> next: " + nextFromStart);

        String nextFromNode1 = transitionResolver.nextNode(plan, "node1", null);
        assertEquals("end", nextFromNode1);
        System.out.println("  - node1 -> next: " + nextFromNode1);
    }

    @Test
    @DisplayName("【单元测试】编译带飞书发送节点的工作流")
    void testWorkflowWithFeishuNode() {
        System.out.println("\n========== 测试：带飞书发送节点的工作流 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("feishu-workflow");
        dsl.setVersion("1.0.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec start = new DslNodeSpec();
        start.setId("start");
        start.setType(DslNodeType.START);
        start.setConfig(new HashMap<>());
        nodes.add(start);

        DslNodeSpec feishu = new DslNodeSpec();
        feishu.setId("sendNotify");
        feishu.setType(DslNodeType.FEISHU_SEND_TEXT);
        Map<String, Object> feishuConfig = new HashMap<>();
        feishuConfig.put("connectionId", 1L);
        feishuConfig.put("chatId", "oc_test123");
        feishuConfig.put("text", "Hello from workflow!");
        feishu.setConfig(feishuConfig);
        nodes.add(feishu);

        DslNodeSpec end = new DslNodeSpec();
        end.setId("end");
        end.setType(DslNodeType.END);
        end.setConfig(Map.of("resultStatus", "NOTIFIED"));
        nodes.add(end);

        dsl.setNodes(nodes);

        List<DslEdgeSpec> edges = new ArrayList<>();
        DslEdgeSpec edge1 = new DslEdgeSpec();
        edge1.setFrom("start");
        edge1.setTo("sendNotify");
        edges.add(edge1);

        DslEdgeSpec edge2 = new DslEdgeSpec();
        edge2.setFrom("sendNotify");
        edge2.setTo("end");
        edges.add(edge2);

        dsl.setEdges(edges);

        ExecutionPlan plan = compiler.compile(dsl);

        System.out.println("✓ 编译成功！");
        System.out.println("  - workflowId: " + plan.getWorkflowId());
        System.out.println("  - 节点数: " + plan.getNodes().size());

        CompiledNode feishuNode = plan.getNodes().get("sendNotify");
        assertNotNull(feishuNode, "飞书节点应该被编译");
        assertEquals(DslNodeType.FEISHU_SEND_TEXT, feishuNode.getType());
        System.out.println("  - 飞书节点类型: " + feishuNode.getType());
        System.out.println("  - 飞书节点配置: " + feishuNode.getConfig());
    }

    @Test
    @DisplayName("【单元测试】缺少START节点应该抛异常")
    void testMissingStartNodeShouldFail() {
        System.out.println("\n========== 测试：缺少START节点应该抛异常 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("no-start-workflow");
        dsl.setVersion("1.0.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec end = new DslNodeSpec();
        end.setId("end");
        end.setType(DslNodeType.END);
        end.setConfig(Map.of("resultStatus", "COMPLETED"));
        nodes.add(end);

        dsl.setNodes(nodes);
        dsl.setEdges(new ArrayList<>());

        System.out.println("DSL: 只有END节点，没有START节点");
        System.out.println("预期: 抛出 RuntimeException");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            compiler.compile(dsl);
        });

        System.out.println("✓ 正确抛出异常: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("START") || ex.getCause().getMessage().contains("START"));
    }

    @Test
    @DisplayName("【单元测试】缺少END节点应该抛异常")
    void testMissingEndNodeShouldFail() {
        System.out.println("\n========== 测试：缺少END节点应该抛异常 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("no-end-workflow");
        dsl.setVersion("1.0.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec start = new DslNodeSpec();
        start.setId("start");
        start.setType(DslNodeType.START);
        start.setConfig(new HashMap<>());
        nodes.add(start);

        dsl.setNodes(nodes);
        dsl.setEdges(new ArrayList<>());

        System.out.println("DSL: 只有START节点，没有END节点");
        System.out.println("预期: 抛出 RuntimeException");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            compiler.compile(dsl);
        });

        System.out.println("✓ 正确抛出异常: " + ex.getMessage());
    }

    @Test
    @DisplayName("【单元测试】有环的工作流应该抛异常")
    void testCyclicWorkflowShouldFail() {
        System.out.println("\n========== 测试：有环的工作流应该抛异常 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("cyclic-workflow");
        dsl.setVersion("1.0.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec start = new DslNodeSpec();
        start.setId("start");
        start.setType(DslNodeType.START);
        start.setConfig(new HashMap<>());
        nodes.add(start);

        DslNodeSpec node1 = new DslNodeSpec();
        node1.setId("node1");
        node1.setType(DslNodeType.END);
        node1.setConfig(Map.of("resultStatus", "DONE"));
        nodes.add(node1);

        dsl.setNodes(nodes);

        List<DslEdgeSpec> edges = new ArrayList<>();

        DslEdgeSpec edge1 = new DslEdgeSpec();
        edge1.setFrom("start");
        edge1.setTo("node1");
        edges.add(edge1);

        DslEdgeSpec edge2 = new DslEdgeSpec();
        edge2.setFrom("node1");
        edge2.setTo("start");
        edges.add(edge2);

        dsl.setEdges(edges);

        System.out.println("DSL: start -> node1 -> start (形成环)");
        System.out.println("预期: 抛出 RuntimeException");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            compiler.compile(dsl);
        });

        System.out.println("✓ 正确抛出异常: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("cycle") || ex.getCause().getMessage().contains("cycle"));
    }

    @Test
    @DisplayName("【单元测试】TransitionResolver 条件分支测试")
    @org.junit.jupiter.api.Disabled("项目中没有 ConditionNodeCompiler，跳过此测试")
    void testTransitionResolverConditionBranch() {
        System.out.println("\n========== 测试：TransitionResolver 条件分支 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("condition-workflow");
        dsl.setVersion("1.0.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec start = new DslNodeSpec();
        start.setId("start");
        start.setType(DslNodeType.START);
        start.setConfig(new HashMap<>());
        nodes.add(start);

        DslNodeSpec condition = new DslNodeSpec();
        condition.setId("condition");
        condition.setType(DslNodeType.CONDITION);
        condition.setConfig(Map.of("condition", "true"));
        nodes.add(condition);

        DslNodeSpec trueBranch = new DslNodeSpec();
        trueBranch.setId("trueNode");
        trueBranch.setType(DslNodeType.END);
        trueBranch.setConfig(Map.of("resultStatus", "TRUE_PATH"));
        nodes.add(trueBranch);

        DslNodeSpec falseBranch = new DslNodeSpec();
        falseBranch.setId("falseNode");
        falseBranch.setType(DslNodeType.END);
        falseBranch.setConfig(Map.of("resultStatus", "FALSE_PATH"));
        nodes.add(falseBranch);

        dsl.setNodes(nodes);

        List<DslEdgeSpec> edges = new ArrayList<>();

        DslEdgeSpec edge1 = new DslEdgeSpec();
        edge1.setFrom("start");
        edge1.setTo("condition");
        edges.add(edge1);

        DslEdgeSpec edge2 = new DslEdgeSpec();
        edge2.setFrom("condition");
        edge2.setTo("trueNode");
        edge2.setConditionKey("true");
        edges.add(edge2);

        DslEdgeSpec edge3 = new DslEdgeSpec();
        edge3.setFrom("condition");
        edge3.setTo("falseNode");
        edge3.setConditionKey("false");
        edges.add(edge3);

        dsl.setEdges(edges);

        ExecutionPlan plan = compiler.compile(dsl);

        System.out.println("✓ 编译成功！");
        System.out.println("  - workflowId: " + plan.getWorkflowId());
        System.out.println("  - 转换数: " + plan.getTransitions().size());

        String nextTrue = transitionResolver.nextNode(plan, "condition", "true");
        assertEquals("trueNode", nextTrue);
        System.out.println("  - condition + 'true' -> " + nextTrue);

        String nextFalse = transitionResolver.nextNode(plan, "condition", "false");
        assertEquals("falseNode", nextFalse);
        System.out.println("  - condition + 'false' -> " + nextFalse);
    }

    @Test
    @DisplayName("【单元测试】重复节点ID应该抛异常")
    void testDuplicateNodeIdShouldFail() {
        System.out.println("\n========== 测试：重复节点ID应该抛异常 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("duplicate-id-workflow");
        dsl.setVersion("1.0.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec start = new DslNodeSpec();
        start.setId("start");
        start.setType(DslNodeType.START);
        start.setConfig(new HashMap<>());
        nodes.add(start);

        DslNodeSpec end = new DslNodeSpec();
        end.setId("start");
        end.setType(DslNodeType.END);
        end.setConfig(Map.of("resultStatus", "COMPLETED"));
        nodes.add(end);

        dsl.setNodes(nodes);
        dsl.setEdges(new ArrayList<>());

        System.out.println("DSL: 两个节点ID都是 'start'");
        System.out.println("预期: 抛出 RuntimeException");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            compiler.compile(dsl);
        });

        System.out.println("✓ 正确抛出异常: " + ex.getMessage());
    }

    @Test
    @DisplayName("【单元测试】无效的边（目标节点不存在）应该抛异常")
    void testInvalidEdgeShouldFail() {
        System.out.println("\n========== 测试：无效的边（目标节点不存在）应该抛异常 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("invalid-edge-workflow");
        dsl.setVersion("1.0.0");

        List<DslNodeSpec> nodes = new ArrayList<>();

        DslNodeSpec start = new DslNodeSpec();
        start.setId("start");
        start.setType(DslNodeType.START);
        start.setConfig(new HashMap<>());
        nodes.add(start);

        dsl.setNodes(nodes);

        List<DslEdgeSpec> edges = new ArrayList<>();

        DslEdgeSpec edge = new DslEdgeSpec();
        edge.setFrom("start");
        edge.setTo("non-existent-node");
        edges.add(edge);

        dsl.setEdges(edges);

        System.out.println("DSL: start -> non-existent-node (目标节点不存在)");
        System.out.println("预期: 抛出 RuntimeException");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            compiler.compile(dsl);
        });

        System.out.println("✓ 正确抛出异常: " + ex.getMessage());
    }
}