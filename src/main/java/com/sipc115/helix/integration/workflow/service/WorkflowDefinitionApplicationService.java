/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.service;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.WorkflowDsl;
import com.sipc115.helix.integration.workflow.spi.DslCompiler;
import com.sipc115.helix.integration.workflow.spi.DslRepository;
import com.sipc115.helix.integration.workflow.spi.ExecutionPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 工作流定义应用服务类
 * <p>
 * 负责工作流定义的保存和编译，将工作流 DSL 转换为可执行的执行计划。
 * 实现了工作流定义的完整生命周期管理，从保存到编译再到存储执行计划。
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Service
public class WorkflowDefinitionApplicationService {
    private final DslRepository dslRepository;

    private final DslCompiler dslCompiler;

    private final ExecutionPlanRepository executionPlanRepository;

    private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionApplicationService.class);

    // ⭐ 新增：注入持久化服务（解耦的核心）
    private final WorkflowPersistenceService persistenceService;

    /**
     * 构造函数
     * <p>
     * 通过依赖注入获取所需的存储库和编译器实例。
     *
     * @param dslRepository DSL 存储库
     * @param dslCompiler DSL 编译器
     * @param executionPlanRepository 执行计划存储库
     * @param persistenceService 工作流持久化服务
     */
    public WorkflowDefinitionApplicationService(DslRepository dslRepository,
                                                DslCompiler dslCompiler,
                                                ExecutionPlanRepository executionPlanRepository,
                                                WorkflowPersistenceService persistenceService) {
        this.dslRepository = dslRepository;
        this.dslCompiler = dslCompiler;
        this.executionPlanRepository = executionPlanRepository;
        this.persistenceService = persistenceService;
    }

    /**
     * 保存并编译工作流 DSL
     * <p>
     * 将工作流 DSL 保存到存储库，然后编译为执行计划，并将执行计划保存到存储库。
     * 编译结果状态（COMPILED/COMPILE_FAILED）会在编译完成后更新。
     *
     * @param dsl 工作流 DSL 对象
     * @return 编译后的执行计划
     */
    public ExecutionPlan saveAndCompile(WorkflowDsl dsl) {
        String workflowId = dsl.getWorkflowId();
        String version = dsl.getVersion();

        try {
            dslRepository.save(dsl);

            ExecutionPlan plan = dslCompiler.compile(dsl);

            if (plan.getPlanId() == null || plan.getPlanId().isEmpty()) {
                String planId = workflowId + "-v" + version + "-" + System.currentTimeMillis();
                plan.setPlanId(planId);
            }

            executionPlanRepository.save(plan);

            persistToDatabase(dsl, plan);

            try {
                persistenceService.updateDslState(
                    workflowId,
                    version,
                    com.sipc115.helix.domain.workflow.WorkflowState.COMPILED.name(),
                    null
                );
            } catch (Exception e) {
                logger.warn("更新编译成功状态失败，继续流程。workflowId={}, version={}", workflowId, version);
            }

            return plan;
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            try {
                persistenceService.updateDslState(
                    workflowId,
                    version,
                    com.sipc115.helix.domain.workflow.WorkflowState.COMPILE_FAILED.name(),
                    errorMsg
                );
            } catch (Exception ex) {
                logger.warn("更新编译失败状态失败。workflowId={}, version={}", workflowId, version);
            }
            throw e;
        }
    }

    /**
     * 持久化到数据库
     * <p>
     * 将 DSL 和执行计划分别保存到 PostgreSQL 数据库。
     * 该方法独立于业务逻辑，只负责数据存储。
     *
     * @param dsl 工作流 DSL 对象
     * @param plan 执行计划对象
     */
    private void persistToDatabase(WorkflowDsl dsl, ExecutionPlan plan) {
        try {
            // 保存 DSL
            persistenceService.saveDsl(dsl);

            // 保存执行计划
            persistenceService.saveExecutionPlan(plan);

            logger.info("✅ 工作流定义已成功持久化到数据库。workflowId={}, version={}",
                       dsl.getWorkflowId(), dsl.getVersion());
        } catch (Exception e) {
            logger.error("❌ 数据库持久化失败，但内存存储仍然有效。workflowId={}, version={}",
                        dsl.getWorkflowId(), dsl.getVersion(), e);
            // 注意：这里不抛出异常，保证向后兼容（即使 DB 失败，内存仍可用）
        }
    }
}
