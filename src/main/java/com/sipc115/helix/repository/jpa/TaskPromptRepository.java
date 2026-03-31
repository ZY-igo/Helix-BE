/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.repository.jpa;

import com.sipc115.helix.domain.entity.TaskPrompt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 任务提示仓库接口
 * <p>
 * 继承自 JpaRepository，提供 TaskPrompt 实体的 CRUD 操作和自定义查询方法。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public interface TaskPromptRepository extends JpaRepository<TaskPrompt, Long> {
    /**
     * 根据任务 ID 查询任务提示
     * <p>
     * 查询指定任务 ID 的所有任务提示信息。
     * 
     * @param taskId 任务 ID
     * @return 任务提示列表
     */
    List<TaskPrompt> findByTaskId(String taskId);
}
