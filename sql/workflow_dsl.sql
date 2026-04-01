-- =====================================================
-- Helix 工作流引擎 - PostgreSQL 表结构
-- =====================================================

-- -----------------------------------------------------
-- 1. 工作流 DSL 表
-- 存储工作流的 DSL 定义（设计态）
-- -----------------------------------------------------
DROP TABLE IF EXISTS workflow_dsl CASCADE;
CREATE TABLE workflow_dsl (
    -- 主键 ID
    id BIGSERIAL PRIMARY KEY,

    -- 工作流 ID（业务主键）
    workflow_id VARCHAR(64) NOT NULL,

    -- 版本号
    version INTEGER NOT NULL,

    -- DSL 内容（JSON 格式）
    dsl_content JSONB NOT NULL,

    -- 状态：DRAFT, PUBLISHED, DEPRECATED
    status VARCHAR(20) DEFAULT 'DRAFT',

    -- 元数据（可选）
    metadata JSONB,

    -- 创建时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 更新时间
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 创建人
    created_by VARCHAR(64),

    -- 更新人
    updated_by VARCHAR(64),

    -- 唯一约束：同一工作流的同一版本只能有一个
    UNIQUE (workflow_id, version)
);

-- 索引优化
CREATE INDEX idx_workflow_dsl_status ON workflow_dsl(status);
CREATE INDEX idx_workflow_dsl_workflow_id ON workflow_dsl(workflow_id);
CREATE INDEX idx_workflow_dsl_metadata ON workflow_dsl USING GIN(metadata);

-- 添加注释
COMMENT ON TABLE workflow_dsl IS '工作流 DSL 定义表';
COMMENT ON COLUMN workflow_dsl.workflow_id IS '工作流 ID（业务主键）';
COMMENT ON COLUMN workflow_dsl.version IS '版本号';
COMMENT ON COLUMN workflow_dsl.dsl_content IS 'DSL 完整内容（JSON 格式）';
COMMENT ON COLUMN workflow_dsl.status IS '状态：DRAFT-草稿，PUBLISHED-已发布，DEPRECATED-已废弃';


-- -----------------------------------------------------
-- 2. 执行计划表
-- 存储编译后的执行计划（运行时）
-- -----------------------------------------------------
DROP TABLE IF EXISTS execution_plan CASCADE;
CREATE TABLE execution_plan (
    -- 主键 ID
    id BIGSERIAL PRIMARY KEY,

    -- 计划 ID（业务主键，唯一标识）
    plan_id VARCHAR(64) NOT NULL UNIQUE,

    -- 关联的工作流 ID
    workflow_id VARCHAR(64) NOT NULL,

    -- 关联的版本号
    version INTEGER NOT NULL,

    -- 执行计划内容（JSON 格式）
    plan_content JSONB NOT NULL,

    -- 编译器版本
    compiler_version VARCHAR(32),

    -- 编译时间
    compiled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 创建时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 创建人
    created_by VARCHAR(64),

    -- 外键约束（可选，保证数据一致性）
    CONSTRAINT fk_execution_plan_dsl
        FOREIGN KEY (workflow_id, version)
        REFERENCES workflow_dsl(workflow_id, version)
        ON DELETE CASCADE
);

-- 索引优化
CREATE INDEX idx_execution_plan_lookup ON execution_plan(workflow_id, version);
CREATE INDEX idx_execution_plan_plan_id ON execution_plan(plan_id);

-- 添加注释
COMMENT ON TABLE execution_plan IS '工作流执行计划表（编译产物）';
COMMENT ON COLUMN execution_plan.plan_id IS '计划 ID（唯一标识）';
COMMENT ON COLUMN execution_plan.workflow_id IS '关联的工作流 ID';
COMMENT ON COLUMN execution_plan.version IS '关联的版本号';
COMMENT ON COLUMN execution_plan.plan_content IS '执行计划完整内容（JSON 格式）';
COMMENT ON COLUMN execution_plan.compiler_version IS '编译器版本';
COMMENT ON COLUMN execution_plan.compiled_at IS '编译时间';


-- -----------------------------------------------------
-- 3. 工作流实例表（可选，用于追踪运行中的实例）
-- -----------------------------------------------------
DROP TABLE IF EXISTS workflow_instance CASCADE;
CREATE TABLE workflow_instance (
    -- 主键 ID
    id BIGSERIAL PRIMARY KEY,

    -- 实例 ID（唯一标识，如 "daily-report-v1-1704067200000"）
    instance_id VARCHAR(64) NOT NULL UNIQUE,

    -- 关联的执行计划 ID
    plan_id VARCHAR(64) NOT NULL,

    -- Temporal 工作流 ID
    temporal_workflow_id VARCHAR(64),

    -- 状态：RUNNING, COMPLETED, FAILED, CANCELLED
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',

    -- 输入参数
    input_data JSONB,

    -- 输出结果
    output_data JSONB,

    -- 当前节点 ID
    current_node_id VARCHAR(64),

    -- 开始时间
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 结束时间
    ended_at TIMESTAMP,

    -- 错误信息
    error_message TEXT,

    -- 外键约束
    CONSTRAINT fk_workflow_instance_plan
        FOREIGN KEY (plan_id)
        REFERENCES execution_plan(plan_id)
);

-- 索引优化
CREATE INDEX idx_workflow_instance_status ON workflow_instance(status);
CREATE INDEX idx_workflow_instance_plan_id ON workflow_instance(plan_id);

-- 添加注释
COMMENT ON TABLE workflow_instance IS '工作流实例运行表';
COMMENT ON COLUMN workflow_instance.instance_id IS '实例 ID（唯一标识）';
COMMENT ON COLUMN workflow_instance.status IS '状态：RUNNING-运行中，COMPLETED-已完成，FAILED-失败，CANCELLED-已取消';