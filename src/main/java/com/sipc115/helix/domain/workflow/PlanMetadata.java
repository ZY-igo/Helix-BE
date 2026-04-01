/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 计划元数据类
 */
@Data
public class PlanMetadata implements Serializable {

    /**
     * DSL 版本号
     */
    private String dslVersion;

    /**
     * Schema 版本
     */
    private String schemaVersion;

    /**
     * 编译时间
     */
    private Instant compiledAt;

    /**
     * 编译器版本
     */
    private String compilerVersion;
}
