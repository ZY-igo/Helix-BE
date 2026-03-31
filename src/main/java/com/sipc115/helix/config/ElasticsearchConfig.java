package com.sipc115.helix.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch配置类
 * <p>
 * 配置Elasticsearch客户端连接和存储库扫描
 * </p>
 * 
 * @author Helix Team
 * @since 1.0.0
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.sipc115.helix.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    /**
     * 配置Elasticsearch客户端
     * <p>
     * 设置Elasticsearch服务器连接信息
     * </p>
     * 
     * @return ClientConfiguration 客户端配置
     */
    @Override
    public ClientConfiguration clientConfiguration() {
        // 构建客户端配置，连接到指定的Elasticsearch服务器
        return ClientConfiguration.builder()
                .connectedTo("192.168.115.23:9200")
                .build();
    }
}
