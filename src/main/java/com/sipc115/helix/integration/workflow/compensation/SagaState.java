/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.compensation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saga 状态管理器
 * <p>
 * 用于管理 Saga 模式中的事务执行状态和补偿逻辑。
 * 每个 Saga 实例都有一个唯一的 sagaId，用于追踪和标识整个分布式事务。
 *
 * <h3>Saga 模式的核心职责：</h3>
 * <ul>
 *   <li>记录已完成的动作 - 跟踪 Saga 中所有成功执行的动作</li>
 *   <li>管理执行上下文 - 在动作之间共享数据</li>
 *   <li>执行补偿操作 - 当失败时按逆序撤销之前的操作</li>
 *   <li>处理补偿失败 - 即使部分补偿失败，也继续执行其他补偿</li>
 * </ul>
 *
 * <h3>线程安全性：</h3>
 * <p>
 * 此类是线程安全的，可以在多线程环境下使用。
 * <ul>
 *   <li>completedActions 使用 synchronizedList 保证线程安全</li>
 *   <li>context 使用 ConcurrentHashMap 保证线程安全</li>
 *   <li>compensating 和 compensationFailed 使用 volatile 保证可见性</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // 1. 创建 Saga 状态管理器
 * SagaState saga = new SagaState("order-12345");
 *
 * // 2. 执行正向操作
 * try {
 *     // 步骤 1: 创建订单
 *     Order order = orderService.create(...);
 *     saga.addCompletedAction(new CreateOrderAction(order.getId()));
 *
 *     // 步骤 2: 扣减库存
 *     inventoryService.deduct(itemId, quantity);
 *     saga.addCompletedAction(new DeductInventoryAction(itemId, quantity));
 *
 *     // 步骤 3: 发送通知
 *     notificationService.send(userId, "订单已创建");
 *     saga.addCompletedAction(new SendNotificationAction(messageId));
 *
 * } catch (Exception e) {
 *     // 3. 失败时执行补偿
 *     saga.compensate();
 * }
 * }
 * </pre>
 *
 * <h3>补偿执行流程：</h3>
 * <pre>
 * 执行补偿 (compensate)
 *       │
 *       ▼
 * 检查是否有已完成的动作
 *       │
 *       ├─── 无 ──→ 直接返回，无需补偿
 *       │
 *       └─── 有 ──→ 继续执行
 *                   │
 *                   ▼
 *           设置 compensating = true
 *                   │
 *                   ▼
 *           按逆序遍历已完成动作
 *           (后执行的动作先补偿)
 *                   │
 *                   ▼
 *           对每个动作：
 *           ├─── 检查 isCompensatable()
 *           │         │
 *           │         ├─── false ──→ 跳过，记录警告
 *           │         │
 *           │         └─── true ──→ 执行 compensate()
 *           │                      │
 *           │                      ├─── 成功 ──→ 继续下一个
 *           │                      │
 *           │                      └─── 失败 ──→ 设置 compensationFailed = true
 *           │                                    继续执行其他补偿
 *           │
 *           ▼
 * 设置 compensating = false
 *       │
 *       ▼
 * 检查是否有补偿失败
 *       │
 *       ├─── 是 ──→ 记录错误日志
 *       │
 *       └─── 否 ──→ 清空已完成动作列表
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see CompensatableAction
 */
public class SagaState {

    /**
     * 日志记录器
     * <p>
     * 用于记录 Saga 的关键操作和调试信息。
     * 静态声明是因为此类可能在非 Spring 管理的环境中使用。
     */
    private static final Logger log = LoggerFactory.getLogger(SagaState.class);

    /**
     * Saga 的唯一标识符
     * <p>
     * 用于追踪和标识整个分布式事务。
     * 建议使用有业务含义的 ID，例如订单号、交易流水号等。
     *
     * <h3>命名建议：</h3>
     * <ul>
     *   <li>order-{orderId} - 订单相关的 Saga</li>
     *   <li>payment-{paymentId} - 支付相关的 Saga</li>
     *   <li>workflow-{executionId}-{nodeId} - 工作流节点相关的 Saga</li>
     * </ul>
     */
    private final String sagaId;

    /**
     * 已完成的补偿动作列表
     * <p>
     * 记录在 Saga 执行过程中所有已成功完成的动作。
     * 当 Saga 失败时，会按逆序对这些动作执行补偿操作。
     *
     * <h3>线程安全性：</h3>
     * <ul>
     *   <li>使用 Collections.synchronizedList() 包装</li>
     *   <li>所有访问和修改操作都是线程安全的</li>
     * </ul>
     *
     * <h3>列表顺序：</h3>
     * <ul>
     *   <li>按执行顺序存储：第一个完成的动作在列表头部</li>
     *   <li>补偿时按逆序执行：最后一个完成的动作先被补偿</li>
     * </ul>
     */
    private final List<CompensatableAction> completedActions;

    /**
     * 执行上下文
     * <p>
     * 用于在 Saga 的不同动作之间共享数据。
     * 例如：第一个动作的输出可以作为第二个动作的输入。
     *
     * <h3>线程安全性：</h3>
     * <ul>
     *   <li>使用 ConcurrentHashMap 保证线程安全</li>
     *   <li>可以安全地进行并发读写</li>
     * </ul>
     *
     * <h3>使用示例：</h3>
     * <pre>
     * {@code
     * // 动作 A 执行后，将结果存入上下文
     * Order order = orderService.create(...);
     * saga.putContext("order", order);
     *
     * // 动作 B 从上下文获取数据
     * Order order = (Order) saga.getContext("order");
     * }
     * </pre>
     */
    private final Map<String, Object> context;

    /**
     * 是否正在执行补偿
     * <p>
     * 标志位，用于防止在补偿过程中添加新的动作。
     * 当补偿开始后，不应该再添加新的已完成动作。
     *
     * <h3>使用volatile保证可见性：</h3>
     * <ul>
     *   <li>一个线程修改后，其他线程能立即看到</li>
     *   <li> Happens-Before 语义保证</li>
     * </ul>
     */
    private volatile boolean compensating;

    /**
     * 补偿是否失败
     * <p>
     * 标志位，记录补偿过程中是否有任何动作补偿失败。
     * 如果有任何补偿失败，整个 Saga 被视为部分补偿成功。
     *
     * <h3>使用volatile保证可见性：</h3>
     * <ul>
     *   <li>一个线程修改后，其他线程能立即看到</li>
     * </ul>
     */
    private volatile boolean compensationFailed;

    /**
     * 构造函数
     * <p>
     * 创建一个新的 Saga 状态管理器实例。
     *
     * @param sagaId Saga 的唯一标识符
     */
    public SagaState(String sagaId) {
        this.sagaId = sagaId;
        this.completedActions = Collections.synchronizedList(new ArrayList<>());
        this.context = new ConcurrentHashMap<>();
        this.compensating = false;
        this.compensationFailed = false;
    }

    /**
     * 获取 Saga 的唯一标识符
     *
     * @return Saga 的 ID
     */
    public String getSagaId() {
        return sagaId;
    }

    /**
     * 添加已完成的动作
     * <p>
     * 当一个动作成功执行后，调用此方法将其添加到已完成列表中。
     * 这样当后续动作失败时，可以对这个动作执行补偿。
     *
     * <h3>线程安全性：</h3>
     * <ul>
     *   <li>方法内部对 completedActions 的访问是线程安全的</li>
     * </ul>
     *
     * <h3>注意事项：</h3>
     * <ul>
     *   <li>如果在补偿过程中调用，会抛出 IllegalStateException</li>
     *   <li>动作 ID 应该唯一，重复添加会覆盖</li>
     * </ul>
     *
     * @param action 已完成的可补偿动作
     * @throws IllegalStateException 如果在补偿过程中调用
     */
    public void addCompletedAction(CompensatableAction action) {
        // 防止在补偿过程中添加新动作
        if (compensating) {
            throw new IllegalStateException(
                "Cannot add action during compensation: " + sagaId +
                ". Action: " + action.getActionId()
            );
        }
        completedActions.add(action);
        log.debug("Saga {}: Added completed action: {}", sagaId, action.getActionId());
    }

    /**
     * 移除最后一个添加的动作
     * <p>
     * 用于某些特殊场景，例如：
     * <ul>
     *   <li>动作执行后需要撤销</li>
     *   <li>需要回退到上一个状态</li>
     * </ul>
     *
     * <h3>使用场景示例：</h3>
     * <pre>
     * {@code
     * // 动作执行后发现需要回退
     * CompensatableAction action = ...;
     * saga.addCompletedAction(action);
     *
     * // 检查是否需要回退
     * if (needRollback) {
     *     saga.removeLastAction();
     *     // 不执行补偿，只是从列表中移除
     * }
     * }
     * </pre>
     *
     * @return true 如果成功移除，false 如果列表为空
     */
    public boolean removeLastAction() {
        if (completedActions.isEmpty()) {
            return false;
        }
        completedActions.remove(completedActions.size() - 1);
        return true;
    }

    /**
     * 执行补偿操作
     * <p>
     * 当 Saga 中的某个动作失败时，调用此方法对之前成功的动作执行补偿。
     * 补偿按照执行顺序的逆序进行，即后执行的动作先被补偿。
     *
     * <h3>执行流程：</h3>
     * <ol>
     *   <li>检查是否有需要补偿的动作</li>
     *   <li>设置 compensating = true</li>
     *   <li>按逆序遍历已完成动作</li>
     *   <li>对每个可补偿的动作执行 compensate()</li>
     *   <li>处理补偿过程中的任何异常</li>
     *   <li>设置 compensating = false</li>
     *   <li>根据补偿结果更新 compensationFailed 标志</li>
     * </ol>
     *
     * <h3>补偿顺序：</h3>
     * <pre>
     * 执行顺序: A → B → C → D → 失败
     * 补偿顺序: D → C → B → A
     * </pre>
     *
     * <h3>异常处理：</h3>
     * <ul>
     *   <li>单个动作的补偿失败不会停止整个补偿流程</li>
     *   <li>补偿失败的动作会记录错误，但继续执行下一个补偿</li>
     *   <li>最终通过 isCompensationFailed() 可以检查是否有任何失败</li>
     * </ul>
     *
     * <h3>注意事项：</h3>
     * <ul>
     *   <li>补偿操作是最大努力，不保证完全成功</li>
     *   <li>如果补偿失败，需要人工介入处理</li>
     *   <li>补偿成功后，completedActions 列表会被清空</li>
     * </ul>
     */
    public void compensate() {
        // 检查是否有需要补偿的动作
        if (completedActions.isEmpty()) {
            log.info("Saga {}: No actions to compensate", sagaId);
            return;
        }

        // 设置补偿状态标志
        compensating = true;
        log.info("Saga {}: Starting compensation of {} actions", sagaId, completedActions.size());

        // 创建副本并反转顺序（后执行的动作先补偿）
        List<CompensatableAction> actionsToCompensate = new ArrayList<>(completedActions);
        Collections.reverse(actionsToCompensate);

        // 按逆序执行补偿
        for (CompensatableAction action : actionsToCompensate) {
            // 检查动作是否可补偿
            if (!action.isCompensatable()) {
                // 不可补偿的动作，记录警告并跳过
                log.warn("Saga {}: Action {} is not compensatable, skipping",
                        sagaId, action.getActionId());
                continue;
            }

            try {
                log.info("Saga {}: Compensating action: {}", sagaId, action.getActionId());
                // 执行补偿操作
                action.compensate();
                log.info("Saga {}: Successfully compensated action: {}", sagaId, action.getActionId());
            } catch (Exception e) {
                // 补偿失败，记录错误但不停止补偿流程
                log.error("Saga {}: Failed to compensate action {}: {}",
                        sagaId, action.getActionId(), e.getMessage());
                // 标记补偿失败
                compensationFailed = true;
                // 继续执行其他补偿（最大努力原则）
            }
        }

        // 补偿流程结束
        compensating = false;

        // 检查是否有任何补偿失败
        if (compensationFailed) {
            log.error("Saga {}: Compensation failed for one or more actions. " +
                     "Manual intervention may be required.", sagaId);
        } else {
            log.info("Saga {}: Compensation completed successfully", sagaId);
            // 补偿成功后清空已完成动作列表
            completedActions.clear();
        }
    }

    /**
     * 存储数据到执行上下文
     * <p>
     * 用于在 Saga 的不同动作之间共享数据。
     * 数据存储在线程安全的 ConcurrentHashMap 中。
     *
     * <h3>使用示例：</h3>
     * <pre>
     * {@code
     * // 存储数据
     * saga.putContext("orderId", "12345");
     * saga.putContext("userId", "67890");
     * saga.putContext("amount", 100.0);
     * }
     * </pre>
     *
     * @param key 数据的键
     * @param value 数据的值
     */
    public void putContext(String key, Object value) {
        context.put(key, value);
    }

    /**
     * 从执行上下文获取数据
     *
     * <h3>使用示例：</h3>
     * <pre>
     * {@code
     * String orderId = (String) saga.getContext("orderId");
     * }
     * </pre>
     *
     * @param key 数据的键
     * @return 数据的值，如果不存在返回 null
     */
    public Object getContext(String key) {
        return context.get(key);
    }

    /**
     * 获取执行上下文的完整副本
     * <p>
     * 返回一个新的 Map，包含上下文中所有数据的副本。
     * 这是为了防止外部对返回的 Map 的修改影响内部状态。
     *
     * @return 上下文的副本
     */
    public Map<String, Object> getAllContext() {
        return new ConcurrentHashMap<>(context);
    }

    /**
     * 检查补偿是否失败
     * <p>
     * 在调用 compensate() 后，可以使用此方法检查补偿过程是否有任何失败。
     *
     * <h3>返回值含义：</h3>
     * <ul>
     *   <li>true - 至少有一个动作的补偿失败了</li>
     *   <li>false - 所有补偿都成功了，或者没有需要补偿的动作</li>
     * </ul>
     *
     * <h3>使用示例：</h3>
     * <pre>
     * {@code
     * saga.compensate();
     * if (saga.isCompensationFailed()) {
     *     // 发送告警通知，需要人工介入
     *     alertService.send("Saga " + saga.getSagaId() + " compensation failed");
     * }
     * }
     * </pre>
     *
     * @return true 表示有补偿失败，false 表示成功
     */
    public boolean isCompensationFailed() {
        return compensationFailed;
    }

    /**
     * 检查是否正在执行补偿
     * <p>
     * 用于在补偿过程中检查状态，防止在补偿时添加新动作。
     *
     * <h3>使用场景：</h3>
     * <pre>
     * {@code
     * // 在 CompensatableAction 实现中
     * public void compensate() {
     *     if (saga.isCompensating()) {
     *         // 可以记录更详细的日志
     *     }
     *     // 执行补偿逻辑
     * }
     * }
     * </pre>
     *
     * @return true 表示正在补偿，false 表示不在补偿状态
     */
    public boolean isCompensating() {
        return compensating;
    }

    /**
     * 获取已完成动作的数量
     *
     * @return 已完成动作的数量
     */
    public int getCompletedActionCount() {
        return completedActions.size();
    }

    /**
     * 获取所有已完成动作的 ID 列表
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>调试和问题排查</li>
     *   <li>日志记录</li>
     *   <li>补偿前的确认</li>
     * </ul>
     *
     * @return 动作 ID 列表
     */
    public List<String> getCompletedActionIds() {
        return completedActions.stream()
                .map(CompensatableAction::getActionId)
                .toList();
    }
}