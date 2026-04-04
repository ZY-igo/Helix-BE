/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.common.constant;

public class ExecutionStatusConstants {

    private ExecutionStatusConstants() {
    }

    public static final String WORKFLOW_RUNNING = "RUNNING";
    public static final String WORKFLOW_SUCCESS = "SUCCESS";
    public static final String WORKFLOW_FAILED = "FAILED";

    public static final String NODE_RUNNING = "RUNNING";
    public static final String NODE_SUCCESS = "SUCCESS";
    public static final String NODE_FAILED = "FAILED";
    public static final String NODE_SKIPPED = "SKIPPED";
    public static final String NODE_TIMED_OUT = "TIMED_OUT";
    public static final String NODE_COMPLETED = "COMPLETED";
}