-- =====================================================
-- Helix 集成连接配置 - PostgreSQL 表结构
-- =====================================================

-- -----------------------------------------------------
-- 1. 集成连接配置表
-- 存储外部服务集成的配置信息（如飞书、LLM、数据库等）
-- -----------------------------------------------------
DROP TABLE IF EXISTS integration_connection CASCADE;
CREATE TABLE integration_connection (
    -- 主键 ID
    id BIGSERIAL PRIMARY KEY,

    -- 连接名称（用户可见）
    name VARCHAR(128) NOT NULL,

    -- 类型：FEISHU, MYSQL, LLM, REDIS等
    type VARCHAR(64) NOT NULL,

    -- 分类：IM, DATABASE, AI等
    category VARCHAR(32),

    -- 配置内容（JSON 格式）
    config JSONB NOT NULL,

    -- 加密后的敏感字段密文
    encrypted_config TEXT,

    -- 状态：ACTIVE, INACTIVE, ERROR
    status VARCHAR(32) DEFAULT 'ACTIVE',

    -- 是否为默认连接
    is_default BOOLEAN DEFAULT FALSE,

    -- 最后测试时间
    last_test_at TIMESTAMP,

    -- 最后测试结果：SUCCESS, FAILED
    last_test_result VARCHAR(32),

    -- 连接描述
    description TEXT,

    -- 创建人
    created_by VARCHAR(64),

    -- 创建时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 更新时间
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 唯一约束：连接名称唯一
    UNIQUE (name)
);

-- 索引优化
CREATE INDEX idx_integration_connection_type_status ON integration_connection(type, status);
CREATE INDEX idx_integration_connection_category ON integration_connection(category);

-- 添加注释
COMMENT ON TABLE integration_connection IS '集成连接配置表';
COMMENT ON COLUMN integration_connection.name IS '连接名称（用户可见）';
COMMENT ON COLUMN integration_connection.type IS '连接类型：FEISHU-飞书，MYSQL-MySQL，LLM-大模型，REDIS-缓存';
COMMENT ON COLUMN integration_connection.category IS '连接分类：IM-即时通讯，DATABASE-数据库，AI-人工智能，CACHE-缓存';
COMMENT ON COLUMN integration_connection.config IS '连接配置内容（JSON 格式）';
COMMENT ON COLUMN integration_connection.encrypted_config IS '加密后的敏感字段密文';
COMMENT ON COLUMN integration_connection.status IS '状态：ACTIVE-活跃，INACTIVE-禁用，ERROR-异常';
COMMENT ON COLUMN integration_connection.is_default IS '是否为默认连接';
COMMENT ON COLUMN integration_connection.last_test_at IS '最后测试时间';
COMMENT ON COLUMN integration_connection.last_test_result IS '最后测试结果：SUCCESS-成功，FAILED-失败';


-- -----------------------------------------------------
-- 2. 集成连接历史记录表
-- 用于审计和追溯配置变更
-- -----------------------------------------------------
DROP TABLE IF EXISTS integration_connection_history CASCADE;
CREATE TABLE integration_connection_history (
    -- 主键 ID
    id BIGSERIAL PRIMARY KEY,

    -- 关联的连接 ID
    connection_id BIGINT NOT NULL,

    -- 历史快照：连接名称
    name VARCHAR(128) NOT NULL,

    -- 历史快照：连接类型
    type VARCHAR(64) NOT NULL,

    -- 历史快照：配置内容（JSON 格式）
    config JSONB NOT NULL,

    -- 历史快照：加密配置
    encrypted_config TEXT,

    -- 历史快照：状态
    status VARCHAR(32),

    -- 变更人
    changed_by VARCHAR(64),

    -- 变更时间
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 变更类型：CREATE, UPDATE, DELETE, TEST
    change_type VARCHAR(32),

    -- 外键约束
    CONSTRAINT fk_connection_history_connection
        FOREIGN KEY (connection_id)
        REFERENCES integration_connection(id)
        ON DELETE CASCADE
);

-- 索引优化
CREATE INDEX idx_connection_history_connection_id ON integration_connection_history(connection_id);
CREATE INDEX idx_connection_history_changed_at ON integration_connection_history(changed_at);

-- 添加注释
COMMENT ON TABLE integration_connection_history IS '集成连接配置历史记录表';
COMMENT ON COLUMN integration_connection_history.connection_id IS '关联的连接 ID';
COMMENT ON COLUMN integration_connection_history.change_type IS '变更类型：CREATE-创建，UPDATE-更新，DELETE-删除，TEST-测试';


-- =====================================================
-- 示例数据
-- =====================================================

-- 飞书连接示例
INSERT INTO integration_connection (name, type, category, config, status, is_default) VALUES
('飞书-人力资源部', 'FEISHU', 'IM', '{
    "appId": "cli_a1b2c3d4",
    "apiBaseUrl": "https://open.feishu.cn/open-apis",
    "chatId": "oc_hr_department"
}', 'ACTIVE', true);

-- LLM 连接示例
INSERT INTO integration_connection (name, type, category, config, status, is_default) VALUES
('智谱AI-标准', 'LLM', 'AI', '{
    "apiKey": "your-api-key-here",
    "baseUrl": "https://open.bigmodel.cn/api/paas/v4",
    "model": "glm-5",
    "temperature": 1.0,
    "maxTokens": 4096
}', 'ACTIVE', true);
