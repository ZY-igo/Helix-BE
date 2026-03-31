/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.integration.lark.FeishuClient;
import com.sipc115.helix.domain.entity.DailyReportStatus;
import com.sipc115.helix.domain.entity.ReportTask;
import com.sipc115.helix.domain.entity.TaskPrompt;
import com.sipc115.helix.repository.jpa.DailyReportStatusRepository;
import com.sipc115.helix.repository.jpa.ReportTaskRepository;
import com.sipc115.helix.repository.jpa.TaskPromptRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 任务执行服务类
 * <p>
 * 负责执行报告任务，生成并发送报告内容到指定的聊天会话。
 * 实现了任务执行的核心逻辑，包括任务查询、状态管理、内容生成和消息发送。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Service
public class TaskExecutionService {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(TaskExecutionService.class);

    /**
     * 报告任务仓库
     * <p>
     * 用于查询和管理报告任务。
     */
    private final ReportTaskRepository reportTaskRepository;
    
    /**
     * 任务提示仓库
     * <p>
     * 用于查询任务相关的提示信息。
     */
    private final TaskPromptRepository taskPromptRepository;
    
    /**
     * 每日报告状态仓库
     * <p>
     * 用于查询和管理每日报告的执行状态。
     */
    private final DailyReportStatusRepository statusRepository;
    
    /**
     * 飞书客户端
     * <p>
     * 用于发送消息到飞书聊天会话。
     */
    private final FeishuClient feishuClient;
    
    /**
     * JSON 对象映射器
     * <p>
     * 用于解析 JSON 格式的会话 ID 列表。
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     * <p>
     * 通过依赖注入获取所需的仓库和客户端实例。
     * 
     * @param reportTaskRepository 报告任务仓库
     * @param taskPromptRepository 任务提示仓库
     * @param statusRepository 每日报告状态仓库
     * @param feishuClient 飞书客户端
     * @param objectMapper JSON 对象映射器
     */
    public TaskExecutionService(
            ReportTaskRepository reportTaskRepository,
            TaskPromptRepository taskPromptRepository,
            DailyReportStatusRepository statusRepository,
            FeishuClient feishuClient,
            ObjectMapper objectMapper) {
        this.reportTaskRepository = reportTaskRepository;
        this.taskPromptRepository = taskPromptRepository;
        this.statusRepository = statusRepository;
        this.feishuClient = feishuClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行任务
     * <p>
     * 根据任务 ID 执行报告任务，生成并发送报告内容到指定的聊天会话。
     * 
     * @param taskId 任务 ID
     * @param force 是否强制执行，忽略执行周期检查
     * @return 每日报告状态
     * @throws IllegalArgumentException 当任务不存在时抛出
     * @throws IllegalStateException 当任务会话 ID 为空时抛出
     */
    @Transactional
    public DailyReportStatus run(String taskId, boolean force) {
        // 获取当前日期
        LocalDate today = LocalDate.now();
        log.info("[TaskExecution] Start run. taskId={}, date={}, force={}", taskId, today, force);

        // 查找任务，不存在则抛出异常
        ReportTask task = reportTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found. taskId=" + taskId));
        
        // 查找任务相关的提示信息
        List<TaskPrompt> prompts = taskPromptRepository.findByTaskId(taskId);

        // 查找或创建每日报告状态
        DailyReportStatus status = statusRepository.findByTaskIdAndReportDate(taskId, today)
                .orElseGet(DailyReportStatus::new);
        status.setTaskId(taskId);
        status.setReportDate(today);

        try {
            if (!force) {
                // TODO: Introduce execution-cycle based duplicate check here.
            }

            // TODO: Replace this placeholder with custom prompt workflow orchestration.
            // 构建占位符广播内容
            String broadcastContent = buildPlaceholderBroadcast(task, prompts, today);
            
            // 解析聊天 ID 列表
            List<String> chatIds = parseChatIds(task.getSessionIds());
            if (chatIds.isEmpty()) {
                throw new IllegalStateException("Task session_ids is empty. taskId=" + taskId);
            }

            // 发送消息到每个聊天会话
            List<String> messageIds = new ArrayList<>();
            for (String chatId : chatIds) {
                messageIds.add(feishuClient.sendTextToChat(chatId, broadcastContent));
            }

            // 更新状态为成功
            status.setSent(true);
            status.setSummaryMessageId(String.join(",", messageIds));
            status.setDetailMessageId(null);
            status.setSentAt(LocalDateTime.now());
            status.setErrorMessage(null);
            return statusRepository.save(status);
        } catch (Exception e) {
            // 更新状态为失败
            status.setSent(false);
            status.setErrorMessage(e.getMessage());
            log.error("[TaskExecution] Run failed. taskId={}, date={}", taskId, today, e);
            return statusRepository.save(status);
        }
    }

    /**
     * 解析聊天 ID 列表
     * <p>
     * 将 JSON 格式的会话 ID 字符串解析为字符串列表。
     * 
     * @param sessionIdsJson JSON 格式的会话 ID 字符串
     * @return 聊天 ID 列表
     * @throws IllegalStateException 当解析失败时抛出
     */
    private List<String> parseChatIds(String sessionIdsJson) {
        if (!StringUtils.hasText(sessionIdsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(sessionIdsJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse task.session_ids JSON: " + e.getMessage(), e);
        }
    }

    /**
     * 构建占位符广播内容
     * <p>
     * 构建临时的占位符广播内容，后续将被自定义提示工作流编排替换。
     * 
     * @param task 报告任务
     * @param prompts 任务提示列表
     * @param date 报告日期
     * @return 广播内容
     */
    private String buildPlaceholderBroadcast(ReportTask task, List<TaskPrompt> prompts, LocalDate date) {
        return """
                Task broadcast triggered
                taskId: %d
                date: %s
                taskIntro: %s
                promptCount: %d

                TODO: execution-cycle validation not implemented
                TODO: custom prompt workflow not implemented
                """.formatted(
                task.getTaskId(),
                date,
                safe(task.getTaskIntro()),
                prompts.size());
    }

    /**
     * 安全处理字符串
     * <p>
     * 处理可能为 null 或空的字符串，确保返回非 null 值。
     * 
     * @param value 输入字符串
     * @return 处理后的字符串，为空时返回空字符串
     */
    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
