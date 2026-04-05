package com.sipc115.helix.integration.connect;

/**
 * 连接客户端接口
 * <p>
 * 只负责连接管理和认证，不涉及具体业务操作。
 * 具体 API 调用由 Activity 层实现。
 */
public interface ConnectionClient<T> {

    /**
     * 获取连接类型
     */
    String getConnectionType();

    /**
     * 测试连接是否可用
     */
    void test(Object config) throws Exception;

    /**
     * 创建认证客户端实例
     * <p>
     * 返回已认证的客户端，供 Activity 层调用具体 API。
     * 
     * @param config 连接配置
     * @return 认证后的客户端实例
     */
    T createClient(Object config) throws Exception;
}
