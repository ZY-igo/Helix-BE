package com.sipc115.helix.integration.workflow.bridge.temporal.activity;

import com.sipc115.helix.domain.workflow.ActivityTaskRequest;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.Map;

/**
 * 活动任务路由活动接口
 * <p>
 * Temporal活动接口，用于分发活动任务请求并返回执行结果
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@ActivityInterface
public interface ActivityTaskRouterActivity {

    /**
     * 分发活动任务请求
     * <p>
     * 接收活动任务请求并执行相应的任务，返回执行结果
     * </p>
     * 
     * @param request 活动任务请求
     * @return 任务执行结果
     */
    @ActivityMethod
    Map<String, Object> dispatch(ActivityTaskRequest request);
}
