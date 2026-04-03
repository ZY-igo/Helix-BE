/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

/**
 * 工作流调度配置类
 */
@Data
public class ScheduleSpec {

    private String cron;

    private String timezone;

    private String type;

    private Long intervalMs;

    private Boolean enabled;
}
