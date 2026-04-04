/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RocketMQProducer {

    @Value("${rocketmq.name-server:192.168.115.23:9876}")
    private String nameServer;

    @Value("${rocketmq.producer.group:workflow-trace-group}")
    private String producerGroup;

    @Value("${rocketmq.producer.send-message-timeout:3000}")
    private int sendMessageTimeout;

    private DefaultMQProducer producer;

    private final ConcurrentHashMap<String, CountDownLatch> pendingMessages = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(sendMessageTimeout);
        producer.setRetryTimesWhenSendFailed(2);
        producer.setRetryAnotherBrokerWhenNotStoreOK(true);

        try {
            producer.start();
            log.info("RocketMQ Producer 启动成功: nameServer={}, group={}", nameServer, producerGroup);
        } catch (Exception e) {
            log.error("RocketMQ Producer 启动失败: {}", e.getMessage(), e);
            throw new RuntimeException("RocketMQ Producer 初始化失败", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (producer != null) {
            producer.shutdown();
            log.info("RocketMQ Producer 已关闭");
        }
    }

    public void sendMessage(String topic, String tags, String body, SendCallback callback) {
        try {
            Message message = new Message(topic, tags, body.getBytes(StandardCharsets.UTF_8));
            producer.send(message, callback);
            log.debug("消息已提交发送: topic={}, tags={}", topic, tags);
        } catch (Exception e) {
            log.error("发送消息失败: topic={}, tags={}, error={}", topic, tags, e.getMessage(), e);
            callback.onException(e);
        }
    }

    public SendResult sendMessageSync(String topic, String tags, String body) throws Exception {
        Message message = new Message(topic, tags, body.getBytes(StandardCharsets.UTF_8));
        SendResult result = producer.send(message);
        log.debug("同步消息发送完成: topic={}, tags={}, sendStatus={}",
                topic, tags, result.getSendStatus());
        return result;
    }

    public boolean sendMessageSyncWithCheck(String topic, String tags, String body,
                                            long timeoutMs) {
        CountDownLatch latch = new CountDownLatch(1);
        pendingMessages.put(body, latch);

        try {
            Message message = new Message(topic, tags, body.getBytes(StandardCharsets.UTF_8));
            producer.send(message, new SendCallback() {
                @Override
                public void onSuccess(SendResult result) {
                    if (result.getSendStatus() == SendStatus.SEND_OK) {
                        log.debug("消息发送成功: topic={}, tags={}, msgId={}",
                                topic, tags, result.getMsgId());
                    } else {
                        log.warn("消息发送状态异常: topic={}, tags={}, status={}",
                                topic, tags, result.getSendStatus());
                    }
                    pendingMessages.remove(body);
                    latch.countDown();
                }

                @Override
                public void onException(Exception e) {
                    log.error("消息发送异常: topic={}, tags={}, error={}",
                            topic, tags, e.getMessage());
                    pendingMessages.remove(body);
                    latch.countDown();
                }
            });

            return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            pendingMessages.remove(body);
            log.error("同步等待发送结果失败: topic={}, tags={}, error={}",
                    topic, tags, e.getMessage());
            return false;
        }
    }
}