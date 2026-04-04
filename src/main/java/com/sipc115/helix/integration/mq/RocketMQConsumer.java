/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Component
public class RocketMQConsumer {

    @Value("${rocketmq.name-server:192.168.115.23:9876}")
    private String nameServer;

    @Value("${rocketmq.consumer.group:workflow-trace-consumer-group}")
    private String consumerGroup;

    private DefaultMQPushConsumer consumer;

    private final ConcurrentHashMap<String, MessageHandler> handlers = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public RocketMQConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() throws MQClientException {
        consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setConsumeThreadMin(4);
        consumer.setConsumeThreadMax(16);
        consumer.setMaxReconsumeTimes(3);
        consumer.setSuspendCurrentQueueTimeMillis(1000);

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consume(List<MessageExt> msgs,
                                                      ConsumeConcurrentlyContext context) {
                if (msgs == null || msgs.isEmpty()) {
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }

                for (MessageExt msg : msgs) {
                    try {
                        String topic = msg.getTopic();
                        String tags = msg.getTags();
                        String body = new String(msg.getBody(), StandardCharsets.UTF_8);

                        log.debug("收到消息: topic={}, tags={}, msgId={}, bodyLen={}",
                                topic, tags, msg.getMsgId(), body.length());

                        String key = topic + ":" + tags;
                        MessageHandler handler = handlers.get(key);

                        if (handler != null) {
                            handler.handle(body);
                        } else {
                            log.warn("没有找到对应的处理器: topic={}, tags={}", topic, tags);
                        }

                    } catch (Exception e) {
                        log.error("处理消息失败: msgId={}, error={}",
                                msg.getMsgId(), e.getMessage(), e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }

                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        log.info("RocketMQ Consumer 初始化成功: nameServer={}, group={}",
                nameServer, consumerGroup);
    }

    @PreDestroy
    public void shutdown() {
        if (consumer != null) {
            consumer.shutdown();
            log.info("RocketMQ Consumer 已关闭");
        }
    }

    public void subscribe(String topic, String tags, MessageHandler handler) {
        try {
            consumer.subscribe(topic, tags);
            handlers.put(topic + ":" + tags, handler);
            log.info("订阅消息: topic={}, tags={}", topic, tags);
        } catch (MQClientException e) {
            log.error("订阅消息失败: topic={}, tags={}, error={}",
                    topic, tags, e.getMessage(), e);
            throw new RuntimeException("订阅消息失败", e);
        }
    }

    public void start() throws MQClientException {
        consumer.start();
        log.info("RocketMQ Consumer 启动成功");
    }

    @FunctionalInterface
    public interface MessageHandler {
        void handle(String messageBody);
    }

    public <T> MessageHandler createJsonHandler(Class<T> clazz, Consumer<T> consumer) {
        return body -> {
            try {
                T obj = objectMapper.readValue(body, clazz);
                consumer.accept(obj);
            } catch (Exception e) {
                log.error("JSON 消息解析失败: class={}, error={}",
                        clazz.getName(), e.getMessage(), e);
            }
        };
    }
}