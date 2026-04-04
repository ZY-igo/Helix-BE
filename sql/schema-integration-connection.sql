-- ============================================
-- 集成连接配置表
-- 用于存储外部服务集成的配置信息
-- ============================================

-- 连接配置表
CREATE TABLE IF NOT EXISTS integration_connection (
    id BIGSERIAL PRIMARY KEY,

    -- 基本信息
    name VARCHAR(128) NOT NULL,           -- 连接名称（用户可见）
    type VARCHAR(64) NOT NULL,             -- 类型：FEISHU, MYSQL, LLM, REDIS等
    category VARCHAR(32),                 -- 分类：IM, DATABASE, AI等

    -- 配置内容（JSON格式）
    config JSONB NOT NULL,

    -- 敏感配置（加密存储）
    encrypted_config TEXT,                -- 加密后的敏感字段密文

    -- 状态
    status VARCHAR(32) DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE, ERROR
    is_default BOOLEAN DEFAULT FALSE,     -- 是否为默认连接
    last_test_at TIMESTAMP,              -- 最后测试时间
    last_test_result VARCHAR(32),         -- 最后测试结果：SUCCESS, FAILED

    -- 元数据
    description TEXT,                     -- 连接描述
    created_by VARCHAR(64),               -- 创建人
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    -- 索引
    CONSTRAINT uk_connection_name UNIQUE (name),
    INDEX idx_type_status (type, status),
    INDEX idx_category (category)
);

-- ============================================
-- 连接配置历史记录表（用于审计）
-- ============================================
CREATE TABLE IF NOT EXISTS integration_connection_history (
    id BIGSERIAL PRIMARY KEY,
    connection_id BIGINT NOT NULL REFERENCES integration_connection(id),

    -- 历史快照
    name VARCHAR(128) NOT NULL,
    type VARCHAR(64) NOT NULL,
    config JSONB NOT NULL,
    encrypted_config TEXT,
    status VARCHAR(32),

    -- 变更信息
    changed_by VARCHAR(64),              -- 变更人
    changed_at TIMESTAMP DEFAULT NOW(),
    change_type VARCHAR(32),              -- CREATE, UPDATE, DELETE, TEST

    INDEX idx_connection_id (connection_id),
    INDEX idx_changed_at (changed_at)
);

-- ============================================
-- 示例数据
-- ============================================

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
