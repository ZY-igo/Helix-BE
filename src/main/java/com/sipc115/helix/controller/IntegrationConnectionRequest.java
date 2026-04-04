/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class IntegrationConnectionRequest {

    @NotBlank(message = "连接名称不能为空")
    private String name;

    @NotBlank(message = "连接类型不能为空")
    private String type;

    private String category;

    @NotNull(message = "配置不能为空")
    private Map<String, Object> config;

    private String description;

    private Boolean isDefault;
}