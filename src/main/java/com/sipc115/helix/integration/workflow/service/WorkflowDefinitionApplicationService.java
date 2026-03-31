/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.service;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.WorkflowDsl;
import com.sipc115.helix.integration.workflow.port.DslCompiler;
import com.sipc115.helix.integration.workflow.port.DslRepository;
import com.sipc115.helix.integration.workflow.port.ExecutionPlanRepository;
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
    /**
     * DSL 存储库
     * <p>
     * 用于存储和检索工作流 DSL 定义。
     */
    private final DslRepository dslRepository;
    
    /**
     * DSL 编译器
     * <p>
     * 用于将工作流 DSL 编译为执行计划。
     */
    private final DslCompiler dslCompiler;
    
    /**
     * 执行计划存储库
     * <p>
     * 用于存储和检索编译后的执行计划。
     */
    private final ExecutionPlanRepository executionPlanRepository;

    /**
     * 构造函数
     * <p>
     * 通过依赖注入获取所需的存储库和编译器实例。
     * 
     * @param dslRepository DSL 存储库
     * @param dslCompiler DSL 编译器
     * @param executionPlanRepository 执行计划存储库
     */
    public WorkflowDefinitionApplicationService(DslRepository dslRepository,
                                                DslCompiler dslCompiler,
                                                ExecutionPlanRepository executionPlanRepository) {
        this.dslRepository = dslRepository;
        this.dslCompiler = dslCompiler;
        this.executionPlanRepository = executionPlanRepository;
    }

    /**
     * 保存并编译工作流 DSL
     * <p>
     * 将工作流 DSL 保存到存储库，然后编译为执行计划，并将执行计划保存到存储库。
     * 
     * @param dsl 工作流 DSL 对象
     * @return 编译后的执行计划
     */
    public ExecutionPlan saveAndCompile(WorkflowDsl dsl) {
        // 保存 DSL 到存储库
        dslRepository.save(dsl);
        
        // 编译 DSL 为执行计划
        ExecutionPlan plan = dslCompiler.compile(dsl);
        
        // 保存执行计划到存储库
        executionPlanRepository.save(plan);
        
        return plan;
    }
}
