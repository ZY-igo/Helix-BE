/**
 * 工作流追踪服务单元测试类
 * <p>
 * 本测试类针对 WorkflowTraceService 的工作流和节点执行追踪功能进行测试。
 * WorkflowTraceService 负责记录工作流执行过程中的各种事件，包括启动、成功、失败等。
 *
 * <h3>WorkflowTraceService 功能说明：</h3>
 * <ul>
 *   <li>工作流执行追踪：记录工作流的启动、成功、失败事件</li>
 *   <li>节点执行追踪：记录节点的启动、成功、失败、跳过事件</li>
 *   <li>MQ消息发送：将追踪事件发送到消息队列</li>
 *   <li>持久化存储：将追踪数据保存到数据库</li>
 * </ul>
 *
 * @see WorkflowTraceService
 * @see com.sipc115.helix.domain.workflow.WorkflowExecutionEntity
 * @see com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity
 */
package com.sipc115.helix.integration.workflow.trace;

import com.sipc115.helix.common.constant.ExecutionStatusConstants;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.domain.workflow.WorkflowExecutionEntity;
import com.sipc115.helix.integration.mq.WorkflowTraceMQService;
import com.sipc115.helix.repository.jpa.NodeExecutionTraceRepository;
import com.sipc115.helix.repository.jpa.WorkflowExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WorkflowTraceService 工作流追踪服务业务逻辑测试
 *
 * <p>
 * WorkflowTraceService 是工作流执行追踪的核心服务，
 * 负责在关键节点记录执行状态，支持：
 * <ul>
 *   <li>实时追踪：通过 MQ 实时发送追踪事件</li>
 *   <li>持久化存储：将追踪数据存入数据库</li>
 *   <li>故障排查：提供完整的执行历史</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class WorkflowTraceServiceTest {

    @Mock
    private WorkflowExecutionRepository executionRepo;

    @Mock
    private NodeExecutionTraceRepository nodeTraceRepo;

    @Mock
    private WorkflowTraceMQService mqService;

    @InjectMocks
    private WorkflowTraceService traceService;

    private WorkflowExecutionEntity workflowExecution;
    private NodeExecutionTraceEntity nodeTrace;

    /**
     * 测试前准备：创建测试用的工作流执行实体和节点追踪实体
     */
    @BeforeEach
    void setUp() {
        workflowExecution = new WorkflowExecutionEntity();
        workflowExecution.setId(1L);
        workflowExecution.setWorkflowId("wf-001");
        workflowExecution.setStatus(ExecutionStatusConstants.WORKFLOW_RUNNING);
        workflowExecution.setStartedAt(Instant.now());

        nodeTrace = new NodeExecutionTraceEntity();
        nodeTrace.setId(1L);
        nodeTrace.setExecutionId(1L);
        nodeTrace.setNodeId("node-001");
        nodeTrace.setStatus(ExecutionStatusConstants.NODE_RUNNING);
        nodeTrace.setStartedAt(Instant.now());
    }

    /**
     * 测试启动工作流执行
     * <p>
     * 验证启动工作流时：
     * <ol>
     *   <li>创建工作流执行记录</li>
     *   <li>保存到数据库</li>
     *   <li>发送 MQ 事件通知</li>
     * </ol>
     *
     * <h3>调用参数：</h3>
     * <ul>
     *   <li>workflowId: 工作流定义ID</li>
     *   <li>version: 工作流版本</li>
     *   <li>input: 工作流输入参数</li>
     *   <li>userId: 操作用户ID</li>
     * </ul>
     */
    @Test
    @DisplayName("测试启动工作流执行")
    void testStartExecution() {
        when(executionRepo.save(any())).thenReturn(workflowExecution);

        WorkflowExecutionEntity result = traceService.startExecution(
                "wf-001", "v1", Map.of("input", "value"), "user-001");

        assertNotNull(result);
        assertEquals("wf-001", result.getWorkflowId());
        assertEquals(ExecutionStatusConstants.WORKFLOW_RUNNING, result.getStatus());
        verify(executionRepo).save(any());
        verify(mqService).sendWorkflowEvent(any());
    }

    /**
     * 测试标记工作流成功
     * <p>
     * 验证工作流执行成功时：
     * <ol>
     *   <li>查询工作流执行记录</li>
     *   <li>更新状态为 WORKFLOW_SUCCESS</li>
     *   <li>设置结束时间</li>
     *   <li>保存更新</li>
     * </ol>
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>状态变为 WORKFLOW_SUCCESS</li>
     *   <li>endedAt 被设置</li>
     * </ul>
     */
    @Test
    @DisplayName("测试标记工作流成功")
    void testMarkExecutionSuccess() {
        when(executionRepo.findById(1L)).thenReturn(Optional.of(workflowExecution));
        when(executionRepo.save(any())).thenReturn(workflowExecution);

        traceService.markExecutionSuccess(1L, Map.of("result", "success"));

        assertEquals(ExecutionStatusConstants.WORKFLOW_SUCCESS, workflowExecution.getStatus());
        assertNotNull(workflowExecution.getEndedAt());
        verify(executionRepo).save(workflowExecution);
    }

    /**
     * 测试标记工作流失败
     * <p>
     * 验证工作流执行失败时：
     * <ol>
     *   <li>查询工作流执行记录</li>
     *   <li>更新状态为 WORKFLOW_FAILED</li>
     *   <li>记录错误信息</li>
     * </ol>
     *
     * <h3>预期行为：</h3>
     * <ul>
     *   <li>状态变为 WORKFLOW_FAILED</li>
     *   <li>errorMessage 被记录</li>
     * </ul>
     */
    @Test
    @DisplayName("测试标记工作流失败")
    void testMarkExecutionFailed() {
        when(executionRepo.findById(1L)).thenReturn(Optional.of(workflowExecution));
        when(executionRepo.save(any())).thenReturn(workflowExecution);

        traceService.markExecutionFailed(1L, "Error occurred");

        assertEquals(ExecutionStatusConstants.WORKFLOW_FAILED, workflowExecution.getStatus());
        assertEquals("Error occurred", workflowExecution.getErrorMessage());
    }

    /**
     * 测试启动节点执行
     * <p>
     * 验证启动节点执行时：
     * <ol>
     *   <li>创建节点追踪记录</li>
     *   <li>保存到数据库</li>
     * </ol>
     *
     * <h3>调用参数：</h3>
     * <ul>
     *   <li>executionId: 工作流执行ID</li>
     *   <li>nodeId: 节点定义ID</li>
     *   <li>nodeType: 节点类型</li>
     *   <li>nodeName: 节点名称</li>
     *   <li>round: 循环轮次</li>
     *   <li>input: 节点输入参数</li>
     * </ul>
     */
    @Test
    @DisplayName("测试启动节点执行")
    void testStartNodeExecution() {
        when(nodeTraceRepo.save(any())).thenReturn(nodeTrace);

        NodeExecutionTraceEntity result = traceService.startNodeExecution(
                1L, "node-001", "START", "START", 1, Map.of("input", "value"));

        assertNotNull(result);
        assertEquals("node-001", result.getNodeId());
        assertEquals(ExecutionStatusConstants.NODE_RUNNING, result.getStatus());
        verify(nodeTraceRepo).save(any());
    }

    /**
     * 测试标记节点成功
     * <p>
     * 验证节点执行成功时：
     * <ol>
     *   <li>查询节点追踪记录</li>
     *   <li>更新状态为 NODE_SUCCESS</li>
     *   <li>设置结束时间</li>
     *   <li>保存更新</li>
     * </ol>
     */
    @Test
    @DisplayName("测试标记节点成功")
    void testMarkNodeSuccess() {
        when(nodeTraceRepo.findById(1L)).thenReturn(Optional.of(nodeTrace));
        when(nodeTraceRepo.save(any())).thenReturn(nodeTrace);

        traceService.markNodeSuccess(1L, Map.of("output", "result"));

        assertEquals(ExecutionStatusConstants.NODE_SUCCESS, nodeTrace.getStatus());
        assertNotNull(nodeTrace.getEndedAt());
        verify(nodeTraceRepo).save(nodeTrace);
    }

    /**
     * 测试标记节点失败
     * <p>
     * 验证节点执行失败时：
     * <ol>
     *   <li>查询节点追踪记录</li>
     *   <li>更新状态为 NODE_FAILED</li>
     *   <li>记录错误信息和堆栈跟踪</li>
     * </ol>
     */
    @Test
    @DisplayName("测试标记节点失败")
    void testMarkNodeFailed() {
        when(nodeTraceRepo.findById(1L)).thenReturn(Optional.of(nodeTrace));
        when(nodeTraceRepo.save(any())).thenReturn(nodeTrace);

        traceService.markNodeFailed(1L, "Node error", "stack trace");

        assertEquals(ExecutionStatusConstants.NODE_FAILED, nodeTrace.getStatus());
        assertEquals("Node error", nodeTrace.getErrorMessage());
        verify(nodeTraceRepo).save(nodeTrace);
    }

    /**
     * 测试标记节点跳过
     * <p>
     * 验证节点被跳过时（如条件不满足）：
     * <ol>
     *   <li>查询节点追踪记录</li>
     *   <li>更新状态为 NODE_SKIPPED</li>
     *   <li>记录跳过原因</li>
     * </ol>
     *
     * <h3>跳过场景：</h3>
     * <ul>
     *   <li>条件节点的结果为 false，不执行后续节点</li>
     *   <li>并行分支中未选中的分支</li>
     *   <li>循环节点达到最大迭代次数</li>
     * </ul>
     */
    @Test
    @DisplayName("测试标记节点跳过")
    void testMarkNodeSkipped() {
        when(nodeTraceRepo.findById(1L)).thenReturn(Optional.of(nodeTrace));
        when(nodeTraceRepo.save(any())).thenReturn(nodeTrace);

        traceService.markNodeSkipped(1L, "Condition not met");

        assertEquals(ExecutionStatusConstants.NODE_SKIPPED, nodeTrace.getStatus());
        verify(nodeTraceRepo).save(nodeTrace);
    }
}