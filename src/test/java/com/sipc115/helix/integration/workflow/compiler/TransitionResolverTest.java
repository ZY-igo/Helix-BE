/**
 * 转换解析器单元测试类
 * <p>
 * 本测试类针对 TransitionResolver 的条件分支解析功能进行测试。
 * TransitionResolver 负责根据当前节点和分支键确定下一个要执行的节点。
 *
 * <h3>TransitionResolver 功能说明：</h3>
 * <ul>
 *   <li>解析条件分支：根据 branchKey（true/false）选择正确的转换路径</li>
 *   <li>处理无条件转换：没有条件键的转换直接返回目标节点</li>
 *   <li>边界情况处理：空列表、null、分支键不匹配等情况</li>
 * </ul>
 *
 * @see TransitionResolver
 * @see com.sipc115.helix.domain.workflow.Transition
 */
package com.sipc115.helix.integration.workflow.compiler;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.Transition;
import com.sipc115.helix.integration.workflow.runtime.TransitionResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 转换解析器业务逻辑测试
 *
 * <p>
 * TransitionResolver 是工作流执行引擎的核心组件，
 * 负责根据当前节点执行结果中的 branchKey 决定下一步流转方向。
 *
 * <h3>转换解析规则：</h3>
 * <ol>
 *   <li>查找所有 from 为当前节点ID的转换</li>
 *   <li>如果有 conditionKey，匹配 conditionKey 与 branchKey</li>
 *   <li>找到匹配的转换后，返回其 to（目标节点ID）</li>
 *   <li>找不到匹配的转换，返回 null</li>
 * </ol>
 */
class TransitionResolverTest {

    private TransitionResolver resolver;
    private ExecutionPlan plan;

    /**
     * 测试前准备：创建 TransitionResolver 实例和空的 ExecutionPlan
     */
    @BeforeEach
    void setUp() {
        resolver = new TransitionResolver();
        plan = new ExecutionPlan();
    }

    /**
     * 测试解析条件分支 - true分支
     * <p>
     * 验证当 branchKey 为 "true" 时，能正确返回对应的目标节点。
     *
     * <h3>测试场景：</h3>
     * <pre>
     *  condition (条件节点)
     *    |
     *    +-- true  --> successNode
     *    |
     *    +-- false --> failNode
     * </pre>
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>nextNode(plan, "condition", "true") 返回 "successNode"</li>
     * </ul>
     */
    @Test
    @DisplayName("测试解析条件分支 - true分支")
    void testResolveTrueBranch() {
        List<Transition> transitions = new ArrayList<>();
        Transition trueTransition = new Transition();
        trueTransition.setFrom("condition");
        trueTransition.setConditionKey("true");
        trueTransition.setTo("successNode");
        transitions.add(trueTransition);

        Transition falseTransition = new Transition();
        falseTransition.setFrom("condition");
        falseTransition.setConditionKey("false");
        falseTransition.setTo("failNode");
        transitions.add(falseTransition);

        plan.setTransitions(transitions);

        String result = resolver.nextNode(plan, "condition", "true");

        assertEquals("successNode", result);
    }

    /**
     * 测试解析条件分支 - false分支
     * <p>
     * 验证当 branchKey 为 "false" 时，能正确返回对应的目标节点。
     *
     * <h3>测试场景：</h3>
     * <pre>
     *  condition (条件节点)
     *    |
     *    +-- true  --> successNode
     *    |
     *    +-- false --> failNode
     * </pre>
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>nextNode(plan, "condition", "false") 返回 "failNode"</li>
     * </ul>
     */
    @Test
    @DisplayName("测试解析条件分支 - false分支")
    void testResolveFalseBranch() {
        List<Transition> transitions = new ArrayList<>();
        Transition trueTransition = new Transition();
        trueTransition.setFrom("condition");
        trueTransition.setConditionKey("true");
        trueTransition.setTo("successNode");
        transitions.add(trueTransition);

        Transition falseTransition = new Transition();
        falseTransition.setFrom("condition");
        falseTransition.setConditionKey("false");
        falseTransition.setTo("failNode");
        transitions.add(falseTransition);

        plan.setTransitions(transitions);

        String result = resolver.nextNode(plan, "condition", "false");

        assertEquals("failNode", result);
    }

    /**
     * 测试无条件转换
     * <p>
     * 验证当转换没有设置 conditionKey 时（无条件转换），
     * 直接返回目标节点。
     *
     * <h3>测试场景：</h3>
     * <pre>
     *  start --> nextNode
     * </pre>
     *
     * <h3>使用场景：</h3>
     * <p>
     * 普通顺序节点之间的转换通常是无条件的，
     * 一旦节点执行完成，就直接流向下一个节点。
     */
    @Test
    @DisplayName("测试无条件转换")
    void testUnconditionalTransition() {
        List<Transition> transitions = new ArrayList<>();
        Transition transition = new Transition();
        transition.setFrom("start");
        transition.setTo("nextNode");
        transitions.add(transition);

        plan.setTransitions(transitions);

        String result = resolver.nextNode(plan, "start", null);

        assertEquals("nextNode", result);
    }

    /**
     * 测试空转换列表
     * <p>
     * 验证当转换列表为空时，返回 null。
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>当没有任何转换时，返回 null</li>
     * </ul>
     *
     * <h3>设计理由：</h3>
     * <p>
     * 返回 null 表示没有后续节点，工作流应该结束。
     */
    @Test
    @DisplayName("测试空转换列表")
    void testEmptyTransitions() {
        plan.setTransitions(new ArrayList<>());

        String result = resolver.nextNode(plan, "anyNode", "any");

        assertNull(result);
    }

    /**
     * 测试无匹配的分支键
     * <p>
     * 验证当存在条件转换，但没有匹配当前 branchKey 时，返回 null。
     *
     * <h3>测试场景：</h3>
     * <ul>
     *   <li>只有一个 conditionKey 为 "only-option" 的转换</li>
     *   <li>查询时使用 "unknown-key"</li>
     *   <li>预期返回 null（没有匹配的转换）</li>
     * </ul>
     *
     * <h3>实际业务场景：</h3>
     * <p>
     * 如果节点返回的 branchKey 与预设的任何条件都不匹配，
     * 说明工作流配置有误或运行时数据异常，应该停止执行。
     */
    @Test
    @DisplayName("测试无匹配的分支键")
    void testNoMatchingBranchKey() {
        List<Transition> transitions = new ArrayList<>();
        Transition transition = new Transition();
        transition.setFrom("condition");
        transition.setConditionKey("only-option");
        transition.setTo("specificNode");
        transitions.add(transition);

        plan.setTransitions(transitions);

        String result = resolver.nextNode(plan, "condition", "unknown-key");

        assertNull(result);
    }

    /**
     * 测试从节点ID不匹配
     * <p>
     * 验证当转换存在，但 from 节点ID与当前节点不匹配时，返回 null。
     *
     * <h3>测试场景：</h3>
     * <ul>
     *   <li>转换 from="start", to="endNode"</li>
     *   <li>查询 from="wrongNode"</li>
     *   <li>预期返回 null（找不到匹配的转换）</li>
     * </ul>
     */
    @Test
    @DisplayName("测试从节点ID不匹配")
    void testFromNodeIdMismatch() {
        List<Transition> transitions = new ArrayList<>();
        Transition transition = new Transition();
        transition.setFrom("start");
        transition.setTo("endNode");
        transitions.add(transition);

        plan.setTransitions(transitions);

        String result = resolver.nextNode(plan, "wrongNode", null);

        assertNull(result);
    }

    /**
     * 测试 null 转换列表
     * <p>
     * 验证当转换列表为 null 时，返回 null。
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>当转换列表为 null 时，返回 null</li>
     * </ul>
     *
     * <h3>设计理由：</h3>
     * <p>
     * null 列表表示没有定义任何转换，应该视为工作流结束。
     */
    @Test
    @DisplayName("测试null转换列表")
    void testNullTransitions() {
        plan.setTransitions(null);

        String result = resolver.nextNode(plan, "anyNode", "any");

        assertNull(result);
    }
}