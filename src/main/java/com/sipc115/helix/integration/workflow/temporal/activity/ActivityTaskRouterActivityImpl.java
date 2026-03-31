/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.temporal.activity;

import com.sipc115.helix.domain.workflow.ActivityTaskRequest;
import com.sipc115.helix.handler.ExternalTaskRouter;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 活动任务路由器活动实现
 * <p>
 * 实现了 ActivityTaskRouterActivity 接口，负责将 Temporal 活动任务路由到外部任务处理器。
 * 作为 Temporal 活动的实现，将活动请求转发给 ExternalTaskRouter 进行处理。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class ActivityTaskRouterActivityImpl implements ActivityTaskRouterActivity {
    /**
     * 外部任务路由器
     * <p>
     * 用于将活动任务分发到对应的处理器。
     */
    private final ExternalTaskRouter externalTaskRouter;

    /**
     * 构造函数
     * <p>
     * 通过依赖注入获取外部任务路由器实例。
     * 
     * @param externalTaskRouter 外部任务路由器
     */
    public ActivityTaskRouterActivityImpl(ExternalTaskRouter externalTaskRouter) {
        this.externalTaskRouter = externalTaskRouter;
    }

    /**
     * 分发活动任务
     * <p>
     * 将活动任务请求转发给外部任务路由器进行处理，并返回处理结果。
     * 
     * @param request 活动任务请求
     * @return 任务处理结果
     */
    @Override
    public Map<String, Object> dispatch(ActivityTaskRequest request) {
        return externalTaskRouter.dispatch(request.getAction(), request.getInput(), request.getConfig());
    }
}
