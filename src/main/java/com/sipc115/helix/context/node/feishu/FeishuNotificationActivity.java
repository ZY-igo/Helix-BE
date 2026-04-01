/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.feishu;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;

/**
 * 飞书通知活动接口
 */
@ActivityInterface
public interface FeishuNotificationActivity {

    @ActivityMethod
    boolean sendText(String chatId, String text);

    @ActivityMethod
    boolean sendPost(String chatId, String title, List<String> lines);

    @ActivityMethod
    boolean sendPostWithLink(String chatId, String title, String text, String url, String linkText);

    @ActivityMethod
    String publishCloudDoc(String title, String content);
}
