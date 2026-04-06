/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResult {

    private boolean success;
    private String message;
    private Long connectionId;
    private String connectionName;
    private String connectionType;
}