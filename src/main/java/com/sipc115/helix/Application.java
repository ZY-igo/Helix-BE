package com.sipc115.helix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 应用程序主类
 * <p>
 * 这是Helix项目的入口类，负责启动Spring Boot应用程序
 * </p>
 *
 * @author Helix Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableAsync
@ConfigurationPropertiesScan
public class Application {

    /**
     * 应用程序主方法
     * <p>
     * 启动Spring Boot应用程序
     * </p>
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
