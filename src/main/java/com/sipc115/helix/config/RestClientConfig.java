package com.sipc115.helix.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * REST 客户端配置类
 * <p>
 * 配置 REST 客户端的超时时间和其他参数
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Configuration
public class RestClientConfig {

    /**
     * 连接超时时间（毫秒）
     */
    @Value("${app.zhipu.connect-timeout:5000}")
    private int connectTimeout;

    /**
     * 读取超时时间（毫秒）
     */
    @Value("${app.zhipu.read-timeout:120000}")
    private int readTimeout;

    /**
     * 创建 REST 客户端构建器
     * 
     * @return REST 客户端构建器
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setConnectionRequestTimeout(connectTimeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setResponseTimeout(readTimeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();

        var httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        var factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);

        return RestClient.builder()
                .requestFactory(factory);
    }
}
