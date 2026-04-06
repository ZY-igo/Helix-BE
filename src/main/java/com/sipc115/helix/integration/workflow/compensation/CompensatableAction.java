/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.compensation;

/**
 * 可补偿动作接口
 * <p>
 * 定义一个可补偿的动作，用于 Saga 模式中的事务补偿。
 * 当 Saga 中的某个动作失败时，之前成功的动作需要按照逆序进行补偿（回滚）。
 *
 * <h3>Saga 模式简介：</h3>
 * <p>
 * Saga 是一种分布式事务模式，用于在多个服务之间保证数据一致性。
 * 与传统的两阶段提交（2PC）不同，Saga 通过定义一系列的"正向操作"和"补偿操作"来实现。
 * 当某个步骤失败时，之前成功的步骤需要按照逆序执行补偿操作。
 *
 * <h3>补偿流程：</h3>
 * <pre>
 * 步骤 1: 动作 A 执行成功 ──────────────────────────────────────▶
 *                  │
 *                  ▼
 * 步骤 2: 动作 B 执行成功 ──────────────────────────────────────▶
 *                  │
 *                  ▼
 * 步骤 3: 动作 C 执行失败 ✗
 *                  │
 *                  ▼
 * 补偿阶段: 动作 B 的补偿操作 ◀──────────────────────────────────
 *                  │
 *                  ▼
 * 补偿阶段: 动作 A 的补偿操作 ◀──────────────────────────────────
 * </pre>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * public class SendNotificationAction implements CompensatableAction {
 *
 *     private final String messageId;
 *     private NotificationService notificationService;
 *     private boolean compensated = false;
 *
 *     @Override
 *     public String getActionId() {
 *         return "sendNotification-" + messageId;
 *     }
 *
 *     @Override
 *     public String getActionType() {
 *         return "NOTIFICATION";
 *     }
 *
 *     @Override
 *     public Object execute() {
 *         // 发送通知
 *         return notificationService.send(messageId);
 *     }
 *
 *     @Override
 *     public void compensate() {
 *         // 撤回通知
 *         notificationService撤回(messageId);
 *         compensated = true;
 *     }
 *
 *     @Override
 *     public boolean isCompensatable() {
 *         return !compensated && notificationService.supports撤回();
 *     }
 * }
 * }
 * </pre>
 *
 * <h3>注意事项：</h3>
 * <ul>
 *   <li>补偿操作应该是幂等的 - 可以安全地多次执行</li>
 *   <li>补偿操作应该是最大努力 - 即使失败也不应该阻塞整个流程</li>
 *   <li>补偿操作应该是可延迟的 - 可以在异步任务中执行</li>
 *   <li>某些操作可能是不可补偿的 - 需要在 isCompensatable() 中声明</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see SagaState
 */
public interface CompensatableAction {

    /**
     * 获取动作的唯一标识符
     * <p>
     * 用于在 Saga 中唯一标识一个动作。
     * 建议使用有意义的 ID，例如包含业务含义的字符串。
     *
     * <h3>ID 命名建议：</h3>
     * <ul>
     *   <li>包含动作类型：sendNotification-12345</li>
     *   <li>包含业务主键：order-create-67890</li>
     *   <li>包含执行顺序：step-1-action-1</li>
     * </ul>
     *
     * <h3>唯一性要求：</h3>
     * <ul>
     *   <li>同一个 Saga 实例中，动作 ID 必须唯一</li>
     *   <li>不同 Saga 实例的动作 ID 可以相同</li>
     * </ul>
     *
     * @return 动作的唯一标识符
     */
    String getActionId();

    /**
     * 获取动作的类型
     * <p>
     * 用于分类和识别动作的类型，方便监控和追踪。
     *
     * <h3>常见类型示例：</h3>
     * <ul>
     *   <li>NOTIFICATION - 发送通知</li>
     *   <li>PAYMENT - 支付</li>
     *   <li>RESERVATION - 预订</li>
     *   <li>DATA_SYNC - 数据同步</li>
     *   <li>RESOURCE_ALLOC - 资源分配</li>
     * </ul>
     *
     * @return 动作类型字符串
     */
    String getActionType();

    /**
     * 执行正向操作
     * <p>
     * 这是 Saga 中实际执行业务逻辑的方法。
     * 例如：发送通知、创建订单、扣减库存等。
     *
     * <h3>执行要求：</h3>
     * <ul>
     *   <li>应该执行业务操作</li>
     *   <li>如果操作失败，应该抛出异常</li>
     *   <li>操作成功后，结果应该被记录用于补偿</li>
     * </ul>
     *
     * <h3>返回值说明：</h3>
     * <p>
     * 返回值是可选的，可以用于：
     * <ul>
     *   <li>记录操作结果（如生成的订单号）</li>
     *   <li>传递给后续步骤使用</li>
     *   <li>用于补偿操作</li>
     * </ul>
     *
     * @return 操作结果，可以为 null
     * @throws Exception 如果操作失败，应该抛出异常
     */
    Object execute();

    /**
     * 执行补偿操作（回滚）
     * <p>
     * 当后续步骤失败时，调用此方法撤销之前执行的操作。
     * 补偿操作应该尽量幂等，因为可能会被多次调用。
     *
     * <h3>补偿原则：</h3>
     * <ol>
     *   <li>幂等性：补偿操作可能被多次调用，必须是幂等的</li>
     *   <li>最大努力：补偿失败通常不应该阻止其他补偿继续执行</li>
     *   <li>可观测性：补偿失败应该被记录和监控</li>
     * </ol>
     *
     * <h3>补偿示例：</h3>
     * <pre>
     * {@code
     * // 发送通知的补偿操作
     * public void compensate() {
     *     // 1. 检查是否已经补偿过（幂等性）
     *     if (alreadyCompensated) {
     *         return;
     *     }
     *
     *     // 2. 执行撤回/取消操作
     *     try {
     *         notificationService.撤回(messageId);
     *     } catch (Exception e) {
     *         // 3. 记录失败，但不要抛出异常（最大努力）
     *         log.error("撤回通知失败: messageId={}", messageId, e);
     *     }
     *
     *     // 4. 标记已补偿
     *     alreadyCompensated = true;
     * }
     * }
     * </pre>
     *
     * @throws Exception 如果补偿失败，通常不应该抛出异常
     */
    void compensate();

    /**
     * 判断此动作是否可补偿
     * <p>
     * 某些操作可能是天然不可逆的，例如：
     * <ul>
     *   <li>发送短信（无法撤回）</li>
     *   <li>发送邮件（取决于邮件服务支持撤回）</li>
     *   <li>现金交易（无法撤销）</li>
     *   <li>已打印的凭证</li>
     * </ul>
     *
     * <h3>返回值含义：</h3>
     * <ul>
     *   <li>true - 动作可以被补偿，失败时会执行 compensate()</li>
     *   <li>false - 动作不可补偿，失败时只能记录并继续</li>
     * </ul>
     *
     * <h3>实现建议：</h3>
     * <pre>
     * {@code
     * @Override
     * public boolean isCompensatable() {
     *     // 依赖于外部服务是否支持撤回
     *     return notificationService.supports撤回();
     * }
     * }
     * </pre>
     *
     * @return true 表示可补偿，false 表示不可补偿
     */
    boolean isCompensatable();

    /**
     * 获取补偿顺序
     * <p>
     * 用于在执行补偿时确定多个动作的补偿顺序。
     * 补偿按照 getCompensationOrder() 的值降序执行（值大的先补偿）。
     *
     * <h3>默认实现：</h3>
     * <p>
     * 默认返回 0。
     * 如果需要自定义补偿顺序，可以在实现类中覆盖此方法。
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>某些动作可能需要优先补偿</li>
     *   <li>补偿顺序可能与执行顺序不同</li>
     *   <li>需要根据业务规则调整补偿顺序</li>
     * </ul>
     *
     * <h3>示例：</h3>
     * <pre>
     * {@code
     * // 订单创建动作 - 优先级高，先补偿
     * public class CreateOrderAction implements CompensatableAction {
     *     @Override
     *     public int getCompensationOrder() {
     *         return 100;  // 高优先级
     *     }
     * }
     *
     * // 库存扣减动作 - 优先级低，后补偿
     * public class DeductInventoryAction implements CompensatableAction {
     *     @Override
     *     public int getCompensationOrder() {
     *         return 50;  // 低优先级
     *     }
     * }
     *
     * // 补偿执行顺序：CreateOrderAction 先补偿，然后是 DeductInventoryAction
     * }
     * </pre>
     *
     * @return 补偿顺序值，值越大越先被补偿，默认为 0
     */
    default int getCompensationOrder() {
        return 0;
    }
}