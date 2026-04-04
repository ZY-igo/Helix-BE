/**
 * DSL编译器端到端测试
 * <p>
 * 拿真实的 DSL 去编译，验证能不能成功。
 * 这是真正的组件测试，不是单元测试。
 */
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
 * DSL编译器端到端测试
 *
 * <p>
 * 测试策略：用真实的 DSL JSON 对象去调用编译器，看能不能编译成功。
 * 不 mock 任何东西，就是真实的 Spring 上下文和真实的编译器。
 */
@SpringBootTest
@ActiveProfiles("test")
class DslCompilerE2ETest {

    @Autowired
    private DslCompiler dslCompiler;

    @Autowired
    private TransitionResolver transitionResolver;

    @Test
    @DisplayName("【E2E】编译最简单的 START -> END 工作流")
    void testSimplestWorkflow() {
        System.out.println("\n========== 测试：最简单的 START -> END 工作流 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("e2e-simplest");
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

        System.out.println("DSL输入: " + dsl.getWorkflowId());
        System.out.println("节点数: " + dsl.getNodes().size());
        System.out.println("边数: " + dsl.getEdges().size());

        ExecutionPlan plan = dslCompiler.compile(dsl);

        System.out.println("✓ 编译成功！");
        System.out.println("  - workflowId: " + plan.getWorkflowId());
        System.out.println("  - entryNodeId: " + plan.getEntryNodeId());
        System.out.println("  - 编译后节点数: " + plan.getNodes().size());
        System.out.println("  - 转换数: " + plan.getTransitions().size());

        assertNotNull(plan);
        assertEquals("e2e-simplest", plan.getWorkflowId());
        assertEquals("start", plan.getEntryNodeId());
        assertEquals(2, plan.getNodes().size());
        assertEquals(1, plan.getTransitions().size());
    }

    @Test
    @DisplayName("【E2E】编译三节点顺序工作流: START -> node1 -> END")
    void testThreeNodeWorkflow() {
        System.out.println("\n========== 测试：三节点顺序工作流 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("e2e-three-nodes");
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

        ExecutionPlan plan = dslCompiler.compile(dsl);

        System.out.println("✓ 编译成功！");
        System.out.println("  - workflowId: " + plan.getWorkflowId());
        System.out.println("  - 入口节点: " + plan.getEntryNodeId());
        System.out.println("  - 节点列表: " + plan.getNodes().keySet());
        System.out.println("  - 转换列表: " + plan.getTransitions().size() + "条");

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
    @DisplayName("【E2E】编译带飞书发送节点的工作流")
    void testWorkflowWithFeishuNode() {
        System.out.println("\n========== 测试：带飞书发送节点的工作流 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("e2e-feishu-workflow");
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

        ExecutionPlan plan = dslCompiler.compile(dsl);

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
    @DisplayName("【E2E】缺少START节点应该抛异常")
    void testMissingStartNodeShouldFail() {
        System.out.println("\n========== 测试：缺少START节点应该抛异常 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("e2e-no-start");
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
        System.out.println("预期: 抛出异常");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            dslCompiler.compile(dsl);
        });

        System.out.println("✓ 正确抛出异常: " + ex.getMessage());
    }

    @Test
    @DisplayName("【E2E】缺少END节点应该抛异常")
    void testMissingEndNodeShouldFail() {
        System.out.println("\n========== 测试：缺少END节点应该抛异常 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("e2e-no-end");
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
        System.out.println("预期: 抛出异常");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            dslCompiler.compile(dsl);
        });

        System.out.println("✓ 正确抛出异常: " + ex.getMessage());
    }

    @Test
    @DisplayName("【E2E】有环的工作流应该抛异常")
    void testCyclicWorkflowShouldFail() {
        System.out.println("\n========== 测试：有环的工作流应该抛异常 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("e2e-cyclic");
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
        System.out.println("预期: 抛出异常");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            dslCompiler.compile(dsl);
        });

        System.out.println("✓ 正确抛出异常: " + ex.getMessage());
    }

    @Test
    @DisplayName("【E2E】TransitionResolver 条件分支测试")
    void testTransitionResolverConditionBranch() {
        System.out.println("\n========== 测试：TransitionResolver 条件分支 ==========");

        WorkflowDsl dsl = new WorkflowDsl();
        dsl.setWorkflowId("e2e-condition");
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
        condition.setConfig(Map.of("condition", "${vars.isTrue}"));
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

        ExecutionPlan plan = dslCompiler.compile(dsl);

        System.out.println("✓ 编译成功！");
        System.out.println("  - 转换数: " + plan.getTransitions().size());

        String nextTrue = transitionResolver.nextNode(plan, "condition", "true");
        assertEquals("trueNode", nextTrue);
        System.out.println("  - condition + 'true' -> " + nextTrue);

        String nextFalse = transitionResolver.nextNode(plan, "condition", "false");
        assertEquals("falseNode", nextFalse);
        System.out.println("  - condition + 'false' -> " + nextFalse);
    }
}