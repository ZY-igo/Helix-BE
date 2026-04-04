package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.ExecutionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 执行上下文运行时状态测试
 *
 * <p>
 * ExecutionContext 是工作流执行时的"全局状态管理器"，
 * 贯穿整个工作流的执行生命周期，负责维护：
 * <ul>
 *   <li>执行计划引用</li>
 *   <li>变量存储（类似作用域变量）</li>
 *   <li>节点状态跟踪</li>
 *   <li>执行进度追踪</li>
 * </ul>
 */
class ExecutionContextTest {

    /**
     * 测试无参构造函数
     * <p>
     * 验证使用无参构造函数创建 ExecutionContext 时，
     * 系统赋予的默认值是否正确。
     *
     * <h3>预期默认值：</h3>
     * <ul>
     *   <li>variables: 非空 Map（用于存储变量）</li>
     *   <li>nodeStatuses: 非空 Map（用于跟踪节点状态）</li>
     *   <li>workflowStatus: PENDING（工作流待执行）</li>
     *   <li>executionOrder: 0（初始执行顺序）</li>
     * </ul>
     */
    @Test
    @DisplayName("测试无参构造函数")
    void testNoArgsConstructor() {
        ExecutionContext context = new ExecutionContext();

        assertNotNull(context.getVariables());
        assertNotNull(context.getNodeStatuses());
        assertEquals(ExecutionStatus.PENDING, context.getWorkflowStatus());
        assertEquals(0, context.getExecutionOrder());
    }

    /**
     * 测试带参数构造函数
     * <p>
     * 验证使用带 ExecutionPlan 和输入参数的构造函数创建 ExecutionContext 时，
     * 计划被正确设置，输入参数被复制到 variables 中。
     *
     * <h3>测试场景：</h3>
     * <ol>
     *   <li>创建 ExecutionPlan 并设置 workflowId</li>
     *   <li>创建输入 Map（key1 -> value1）</li>
     *   <li>使用构造函数创建 ExecutionContext</li>
     *   <li>验证 plan 引用正确</li>
     *   <li>验证输入参数被复制到 variables</li>
     * </ol>
     */
    @Test
    @DisplayName("测试带参数构造函数")
    void testConstructorWithPlanAndInput() {
        ExecutionPlan plan = new ExecutionPlan();
        plan.setWorkflowId("test-workflow");
        Map<String, Object> input = new HashMap<>();
        input.put("key1", "value1");

        ExecutionContext context = new ExecutionContext(plan, input);

        assertEquals(plan, context.getPlan());
        assertEquals("value1", context.getVariables().get("key1"));
    }

    /**
     * 测试带参数构造函数处理 null 输入
     * <p>
     * 验证当输入参数为 null 时，构造函数能正确处理。
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>plan 被正确设置</li>
     *   <li>variables 为非空空 Map（不是 null）</li>
     * </ul>
     */
    @Test
    @DisplayName("测试带参数构造函数处理null输入")
    void testConstructorWithNullInput() {
        ExecutionPlan plan = new ExecutionPlan();
        ExecutionContext context = new ExecutionContext(plan, null);

        assertEquals(plan, context.getPlan());
        assertNotNull(context.getVariables());
        assertTrue(context.getVariables().isEmpty());
    }

    /**
     * 测试设置和获取执行计划
     * <p>
     * 验证 ExecutionContext 的 plan 字段能正确设置和获取。
     *
     * <h3>plan 的作用：</h3>
     * <p>
     * ExecutionPlan 包含工作流的完整定义：
     * 节点列表、转换关系、入口节点等。
     */
    @Test
    @DisplayName("测试设置和获取执行计划")
    void testSetAndGetPlan() {
        ExecutionContext context = new ExecutionContext();
        ExecutionPlan plan = new ExecutionPlan();
        plan.setWorkflowId("test-plan");

        context.setPlan(plan);

        assertEquals(plan, context.getPlan());
    }

    /**
     * 测试设置和获取变量
     * <p>
     * 验证 ExecutionContext 的 variables 字段能正确存储和返回变量。
     *
     * <h3>variables 的用途：</h3>
     * <ul>
     *   <li>存储工作流的输入参数</li>
     *   <li>存储节点执行产生的中间结果</li>
     *   <li>存储循环变量等临时数据</li>
     * </ul>
     */
    @Test
    @DisplayName("测试设置和获取变量")
    void testSetAndGetVariables() {
        ExecutionContext context = new ExecutionContext();
        Map<String, Object> vars = new HashMap<>();
        vars.put("var1", "value1");

        context.setVariables(vars);

        assertEquals("value1", context.getVariables().get("var1"));
    }

    /**
     * 测试设置和获取节点状态
     * <p>
     * 验证 ExecutionContext 的 nodeStatuses 字段能正确记录各节点状态。
     *
     * <h3>nodeStatuses 的结构：</h3>
     * <p>
     * Map<节点ID, 节点状态>
     * <br>
     * 例如：{"node-001": "COMPLETED", "node-002": "RUNNING"}
     *
     * <h3>使用场景：</h3>
     * <p>
     * 工作流引擎通过 nodeStatuses 跟踪每个节点的执行进度，
     * 便于实现节点重试、状态恢复等功能。
     */
    @Test
    @DisplayName("测试设置和获取节点状态")
    void testSetAndGetNodeStatuses() {
        ExecutionContext context = new ExecutionContext();
        Map<String, ExecutionStatus> statuses = new HashMap<>();
        statuses.put("node1", ExecutionStatus.COMPLETED);

        context.setNodeStatuses(statuses);

        assertEquals(ExecutionStatus.COMPLETED, context.getNodeStatuses().get("node1"));
    }

    /**
     * 测试设置和获取当前节点ID
     * <p>
     * 验证 ExecutionContext 的 currentNodeId 字段能正确设置和获取。
     *
     * <h3>currentNodeId 的用途：</h3>
     * <ul>
     *   <li>标识当前正在执行的节点</li>
     *   <li>用于日志记录和调试</li>
     *   <li>用于断点续执等高级功能</li>
     * </ul>
     */
    @Test
    @DisplayName("测试设置和获取当前节点ID")
    void testSetAndGetCurrentNodeId() {
        ExecutionContext context = new ExecutionContext();

        context.setCurrentNodeId("node-001");

        assertEquals("node-001", context.getCurrentNodeId());
    }

    /**
     * 测试设置和获取工作流状态
     * <p>
     * 验证 ExecutionContext 的 workflowStatus 字段能正确反映整个工作流的状态。
     *
     * <h3>工作流状态流转：</h3>
     * <ol>
     *   <li>PENDING（待执行）-> RUNNING（运行中）</li>
     *   <li>RUNNING -> COMPLETED（完成）或 FAILED（失败）</li>
     *   <li>或其他中间状态如 WAITING_SIGNAL</li>
     * </ol>
     */
    @Test
    @DisplayName("测试设置和获取工作流状态")
    void testSetAndGetWorkflowStatus() {
        ExecutionContext context = new ExecutionContext();

        context.setWorkflowStatus(ExecutionStatus.RUNNING);

        assertEquals(ExecutionStatus.RUNNING, context.getWorkflowStatus());
    }

    /**
     * 测试设置和获取执行ID
     * <p>
     * 验证 ExecutionContext 的 executionId 字段能正确设置和获取。
     *
     * <h3>executionId 的用途：</h3>
     * <ul>
     *   <li>关联到数据库中的执行记录</li>
     *   <li>用于日志和追踪</li>
     *   <li>支持执行记录的查询和审计</li>
     * </ul>
     */
    @Test
    @DisplayName("测试设置和获取执行ID")
    void testSetAndGetExecutionId() {
        ExecutionContext context = new ExecutionContext();

        context.setExecutionId(123L);

        assertEquals(123L, context.getExecutionId());
    }

    /**
     * 测试设置和获取执行顺序
     * <p>
     * 验证 ExecutionContext 的 executionOrder 字段能正确设置和获取。
     *
     * <h3>executionOrder 的用途：</h3>
     * <p>
     * 记录工作流被"唤醒"执行的次数。
     * 每次工作流从等待状态恢复执行时，executionOrder 加一。
     * 这对于调试和理解工作流的执行历史非常有用。
     */
    @Test
    @DisplayName("测试设置和获取执行顺序")
    void testSetAndGetExecutionOrder() {
        ExecutionContext context = new ExecutionContext();

        context.setExecutionOrder(5);

        assertEquals(5, context.getExecutionOrder());
    }

    /**
     * 测试递增执行顺序
     * <p>
     * 验证 ExecutionContext 的 incrementExecutionOrder() 方法能正确递增执行顺序。
     *
     * <h3>使用场景：</h3>
     * <p>
     * 每次工作流被调度器唤醒继续执行时，
     * 工作流引擎调用 incrementExecutionOrder() 记录一次"唤醒"。
     *
     * <h3>测试步骤：</h3>
     * <ol>
     *   <li>初始 executionOrder 为 0</li>
     *   <li>第一次递增后变为 1</li>
     *   <li>第二次递增后变为 2</li>
     * </ol>
     */
    @Test
    @DisplayName("测试递增执行顺序")
    void testIncrementExecutionOrder() {
        ExecutionContext context = new ExecutionContext();

        assertEquals(0, context.getExecutionOrder());

        context.incrementExecutionOrder();
        assertEquals(1, context.getExecutionOrder());

        context.incrementExecutionOrder();
        assertEquals(2, context.getExecutionOrder());
    }

    /**
     * 测试设置和获取当前节点TraceId
     * <p>
     * 验证 ExecutionContext 的 currentNodeTraceId 字段能正确设置和获取。
     *
     * <h3>currentNodeTraceId 的用途：</h3>
     * <ul>
     *   <li>关联到节点执行轨迹记录</li>
     *   <li>用于追踪节点级别的执行细节</li>
     *   <li>支持节点执行历史的查询</li>
     * </ul>
     */
    @Test
    @DisplayName("测试设置和获取当前节点TraceId")
    void testSetAndGetCurrentNodeTraceId() {
        ExecutionContext context = new ExecutionContext();

        context.setCurrentNodeTraceId(456L);

        assertEquals(456L, context.getCurrentNodeTraceId());
    }

    /**
     * 测试 toView 方法
     * <p>
     * 验证 ExecutionContext 的 toView() 方法能正确转换为视图对象。
     *
     * <h3>toView 方法的作用：</h3>
     * <p>
     * 将 ExecutionContext 转换为只读的视图对象，
     * 用于 API 返回或日志记录，避免直接暴露内部状态。
     *
     * <h3>测试验证：</h3>
     * <ul>
     *   <li>workflowId 正确传递到视图</li>
     *   <li>currentNodeId 正确传递到视图</li>
     *   <li>status 正确传递到视图</li>
     *   <li>variables 正确传递到视图</li>
     * </ul>
     */
    @Test
    @DisplayName("测试toView方法")
    void testToView() {
        ExecutionPlan plan = new ExecutionPlan();
        plan.setWorkflowId("test-workflow");

        ExecutionContext context = new ExecutionContext(plan, null);
        context.setCurrentNodeId("current-node");
        context.setWorkflowStatus(ExecutionStatus.RUNNING);
        context.getVariables().put("var1", "value1");

        var view = context.toView();

        assertEquals("test-workflow", view.getWorkflowId());
        assertEquals("current-node", view.getCurrentNodeId());
        assertEquals(ExecutionStatus.RUNNING, view.getStatus());
        assertEquals("value1", view.getVariables().get("var1"));
    }
}