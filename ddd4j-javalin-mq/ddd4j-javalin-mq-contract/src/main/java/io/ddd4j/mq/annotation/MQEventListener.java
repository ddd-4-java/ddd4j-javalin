package io.ddd4j.mq.annotation;

import java.lang.annotation.*;

/**
 * MQ 事件监听器注解。
 *
 * <p>标注在 Bean 的方法上，声明该方法为 MQ 消费端点。方法的首个参数类型即为
 * 消息反序列化目标类型（通常为 {@code io.ddd4j.core.event.Event} 的子类）。
 *
 * <h3>路由模型</h3>
 * <pre>
 *   namespace.topic.tag
 *   └───┬───┘ └─┬─┘ └┬┘
 *    环境隔离 业务分类 细分标签
 * </pre>
 * <ul>
 *   <li>{@code namespace} —— 命名空间，用于多环境 / 多租户隔离</li>
 *   <li>{@code topic} —— 消费线程隔离维度，不同 topic 走不同消费线程池</li>
 *   <li>{@code tag} —— 同 topic 下共享消费线程，做消息过滤</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @MQEventListener(topic = "order", tags = "paid")
 * public void onOrderPaid(OrderPaidEvent event) {
 *     // 消费 order.paid 的消息
 * }
 *
 * @MQEventListener(topic = "order", tags = "paid || shipped -cancelled")
 * public void onOrderActive(OrderEvent event) {
 *     // 消费 paid 或 shipped，排除 cancelled
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MQEventListener {

    /**
     * 消费者组。
     *
     * <p>同组的多实例共享消费（每条消息只被一个实例消费），不同组各自独立消费全量。
     * 为空时默认 {@code ${应用名}_${方法名}}。
     *
     * @return 消费者组名，空串表示使用默认值
     */
    String group() default "";

    /**
     * 命名空间，用于多环境 / 多租户隔离。
     *
     * <p>为空时取全局配置 {@code ddd4j.mq.namespace}。
     *
     * @return 命名空间，空串表示使用全局配置
     */
    String namespace() default "";

    /**
     * 主题，消费线程隔离维度。
     *
     * <p>不同 topic 的消息走不同消费线程池，互不阻塞。
     *
     * @return 主题名，默认 {@code "DEFAULT"}
     */
    String topic() default "DEFAULT";

    /**
     * 标签表达式，同一 topic 下做消息过滤。
     *
     * <p>支持的语法：
     * <ul>
     *   <li>{@code *} —— 匹配所有（默认）</li>
     *   <li>{@code paid} —— 精确匹配单个标签</li>
     *   <li>{@code paid || shipped} —— 匹配多个标签之一</li>
     *   <li>{@code * -cancelled} —— 匹配所有但排除指定标签</li>
     * </ul>
     *
     * @return 标签表达式，默认 {@code "*"}
     */
    String tags() default "*";

    /**
     * 策略过滤，业务层面的二次筛选。
     *
     * <p>即使 topic 和 tag 匹配通过，消息的 {@code supports()} 方法
     * 返回值仍需命中此列表才会被消费。为 {@code {"*"}} 时放行所有。
     *
     * @return 支持的策略列表，默认 {@code {"*"}}
     */
    String[] supports() default "*";

    /**
     * 分隔符，用于拼接 {@code namespace.topic.tag} 物理地址。
     *
     * <p>为空时由各 Broker Adapter 决定默认分隔符：
     * <ul>
     *   <li>RabbitMQ / RocketMQ —— {@code "."}</li>
     *   <li>Redis Stream —— {@code ":"}</li>
     *   <li>Kafka —— {@code "_"}</li>
     *   <li>MQTT —— {@code "/"}</li>
     * </ul>
     *
     * @return 分隔符，空串表示使用 Broker 默认值
     */
    String separator() default "";
}
