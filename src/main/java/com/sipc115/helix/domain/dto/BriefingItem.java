/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 简报项目类
 * <p>
 * 用于表示简报中的单个项目信息，包含标题、来源、URL、核心内容和功能影响等字段。
 * 使用 Jackson 注解忽略未知属性，提高序列化和反序列化的灵活性。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BriefingItem {

    /**
     * 标题
     * <p>
     * 简报项目的标题，用于标识项目的主题。
     */
    private String title;
    
    /**
     * 来源
     * <p>
     * 简报项目的来源，如网站、文档等。
     */
    private String source;
    
    /**
     * URL
     * <p>
     * 简报项目的链接地址，用于访问原始内容。
     */
    private String url;
    
    /**
     * 核心内容
     * <p>
     * 简报项目的核心内容，包含主要信息。
     */
    private String coreContent;
    
    /**
     * 功能影响
     * <p>
     * 简报项目对功能的影响描述。
     */
    private String functionImpact;

    /**
     * 获取标题
     * 
     * @return 标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置标题
     * 
     * @param title 标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取来源
     * 
     * @return 来源
     */
    public String getSource() {
        return source;
    }

    /**
     * 设置来源
     * 
     * @param source 来源
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * 获取 URL
     * 
     * @return URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * 设置 URL
     * 
     * @param url URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 获取核心内容
     * 
     * @return 核心内容
     */
    public String getCoreContent() {
        return coreContent;
    }

    /**
     * 设置核心内容
     * 
     * @param coreContent 核心内容
     */
    public void setCoreContent(String coreContent) {
        this.coreContent = coreContent;
    }

    /**
     * 获取功能影响
     * 
     * @return 功能影响
     */
    public String getFunctionImpact() {
        return functionImpact;
    }

    /**
     * 设置功能影响
     * 
     * @param functionImpact 功能影响
     */
    public void setFunctionImpact(String functionImpact) {
        this.functionImpact = functionImpact;
    }

}
