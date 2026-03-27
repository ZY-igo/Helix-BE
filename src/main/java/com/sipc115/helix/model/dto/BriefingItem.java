package com.sipc115.helix.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BriefingItem {

    private String title;
    private String source;
    private String url;
    private String coreContent;
    private String functionImpact;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCoreContent() {
        return coreContent;
    }

    public void setCoreContent(String coreContent) {
        this.coreContent = coreContent;
    }

    public String getFunctionImpact() {
        return functionImpact;
    }

    public void setFunctionImpact(String functionImpact) {
        this.functionImpact = functionImpact;
    }

}
