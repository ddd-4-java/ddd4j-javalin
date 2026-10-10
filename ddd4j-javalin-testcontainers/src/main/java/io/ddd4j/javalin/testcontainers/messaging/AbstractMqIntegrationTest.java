package io.ddd4j.javalin.testcontainers.messaging;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.ddd4j.javalin.mq.core.Ddd4jJavalinMqProperties;
import io.ddd4j.javalin.mq.core.JavalinMqLifecycleParticipant;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.serialization.JsonMQEventSerialization;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * MQ broker 集成测试基类：抽取 10 个 ddd4j-javalin-mq-* IT 的公共骨架
 * （容器 start/stop、Guice 装配断言、publish → broker → consume round-trip），
 * broker 差异全部收敛到抽象方法与钩子。
 *
 * <p>子类只需提供：静态容器、{@link #brokerName()}、{@link #topicName()}、
 * 基于已启动容器构造 properties / client / Guice module 的工厂方法。
 * 默认行为可被以下钩子覆盖：</p>
 * <ul>
 *   <li>{@link #tagName()} — 事件 tag；返回 {@code null} 表示发布不带 tag（Pulsar）</li>
 *   <li>{@link #listenerTags()} — listener 订阅 tags，默认与 {@link #tagName()} 一致</li>
 *   <li>{@link #consumerGroup()} / {@link #consumerSettleDelay()} / {@link #awaitTimeout()}</li>
 *   <li>{@link #preInit} — init 之前的 broker 准备（RocketMQ producer warm-up、RabbitMQ exchange）</li>
 *   <li>{@link #adaptListenerTopic} — init 之前改写 listener topic（SQS queue URL）</li>
 * </ul>
 *
 * <p>注：listener 的 topic/group/tags 不依赖注解常量（注解值无法按子类参数化），
 * 而是在 {@link MQListener#of} 之后用 setter 编程式写入，注解仅作占位。</p>
 *
 * @param <P> broker 特定的 properties 类型
 * @param <C> broker 特定的 client 类型
 */
public abstract class AbstractMqIntegrationTest<P extends MQProperties, C extends MQClient> {

    /** 统一 smoke tag。 */
    protected static final String TAG = "smoke";

    /**
     * 子类提供 broker 名称，写入 {@link MQProperties#setBroker} 与 consumer group 前缀。
     *
     * @return broker 名称（如 kafka、rabbitmq、rocketmq）
     */
    protected abstract String brokerName();

    /**
     * 子类提供测试主题名，round-trip 发布与订阅均使用该主题。
     *
     * @return 测试主题名
     */
    protected abstract String topicName();

    /**
     * 子类持有的静态容器（每个测试方法 start/stop）。
     *
     * @return 测试容器实例
     */
    protected abstract GenericContainer<?> container();

    /**
     * 基于已启动容器构造 broker properties（含连接地址与凭证）。
     *
     * @return broker 特定的连接属性
     */
    protected abstract P newProperties();

    /**
     * 基于 broker properties 构造对应的客户端实例。
     *
     * @param props broker 特定的连接属性
     * @return broker 特定的客户端实例
     */
    protected abstract C newClient(P props);

    /**
     * 组装客户端与属性对应的 Guice 模块，供契约装配断言使用。
     *
     * @param client broker 特定的客户端实例
     * @param props  broker 特定的连接属性
     * @return 装配 MQ 契约的 Guice 模块
     */
    protected abstract Module guiceModule(C client, P props);

    /**
     * 客户端具体类型，供 {@code injector.getInstance} 契约断言使用。
     *
     * @return 客户端具体类型
     */
    protected abstract Class<C> clientClass();

    /**
     * consumer group，缺省为 {@code it-<broker>-consumer}。
     *
     * @return consumer group 名称
     */
    protected String consumerGroup() {
        return "it-" + brokerName() + "-consumer";
    }

    /**
     * 事件 tag；{@code null} 表示发布与断言均不带 tag。
     *
     * @return 事件 tag，或 {@code null}
     */
    protected String tagName() {
        return TAG;
    }

    /**
     * listener 订阅 tags，默认跟随 {@link #tagName()}；Pulsar 覆写为 {@code *}。
     *
     * @return listener 订阅 tags
     */
    protected String listenerTags() {
        return tagName();
    }

    /**
     * consumer init 完成后、publish 之前的稳定等待。
     *
     * @return 稳定等待时长，默认 3 秒
     */
    protected Duration consumerSettleDelay() {
        return Duration.ofSeconds(3);
    }

    /**
     * round-trip 断言的最长等待时长。
     *
     * @return 最长等待时长，默认 20 秒
     */
    protected Duration awaitTimeout() {
        return Duration.ofSeconds(20);
    }

    /**
     * init 之前的 broker 准备钩子（在 client 创建之后、Guice 装配之前调用）。
     *
     * @param client   broker 特定的客户端实例
     * @param props    broker 特定的连接属性
     * @param mqProps  ddd4j 侧 MQ 开关与 broker 标识属性
     * @throws Exception broker 准备失败
     */
    protected void preInit(C client, P props, MQProperties mqProps) throws Exception {
    }

    /**
     * init 之前改写 listener topic 的钩子（默认使用 {@link #topicName()}）。
     *
     * @param listener 待改写 topic 的监听器
     * @param props    broker 特定的连接属性
     */
    protected void adaptListenerTopic(MQListener listener, P props) {
    }

    @Test
    void shouldResolveCoreContractsFromGuice() {
        container().start();
        try {
            P brokerProps = newProperties();

            Injector injector = Guice.createInjector(guiceModule(newClient(brokerProps), brokerProps));

            assertThat(injector.getInstance(MQClient.class)).isNotNull();
            assertThat(injector.getInstance(clientClass())).isNotNull();
            assertThat(injector.getInstance(MQProperties.class)).isSameAs(brokerProps);
        } finally {
            container().stop();
        }
    }

    @Test
    void shouldPublishAndConsumeRoundTrip() throws Exception {
        container().start();
        try {
            P brokerProps = newProperties();
            MQProperties mqProps = new MQProperties();
            mqProps.setEnabled(true);
            mqProps.setBroker(brokerName());
            mqProps.setPersist(false);

            C client = newClient(brokerProps);
            preInit(client, brokerProps, mqProps);

            SmokeListener bean = new SmokeListener();
            Method onSmoke = SmokeListener.class.getMethod("onSmoke", MQEvent.class);
            MQListener listener = MQListener.of(bean, onSmoke, onSmoke.getAnnotation(MQEventListener.class));
            listener.setTopic(topicName());
            listener.setGroup(consumerGroup());
            listener.setTags(listenerTags());
            adaptListenerTopic(listener, brokerProps);
            Ddd4jJavalinMqProperties integrationProperties = new Ddd4jJavalinMqProperties();
            JavalinMqLifecycleParticipant lifecycle = new JavalinMqLifecycleParticipant(
                    client, mqProps, integrationProperties, () -> List.of(listener),
                    new JsonMQEventSerialization(), null);
            lifecycle.start();
            try {
                // Give the consumer a moment to finish connect / subscribe / rebalance.
                Thread.sleep(consumerSettleDelay().toMillis());

                MQEvent event = new MQEvent();
                event.setMsgId(brokerName() + "-it-" + System.nanoTime());
                event.setTopic(listener.getTopic());
                if (tagName() != null) {
                    event.setTag(tagName());
                }
                event.publish();

                await().atMost(awaitTimeout()).until(() -> bean.received.get() != null);
                MQEvent received = bean.received.get();
                assertThat(received.getMsgId()).isEqualTo(event.getMsgId());
                assertThat(received.getTopic()).isEqualTo(listener.getTopic());
                if (tagName() != null) {
                    assertThat(received.getTag()).isEqualTo(tagName());
                }
            } finally {
                lifecycle.close();
            }
        } finally {
            container().stop();
        }
    }

    /**
     * Listener bean invoked by the ddd4j consume pipeline; records the delivered event.
     * 注解仅作 {@link MQListener#of} 的占位，topic/group/tags 由基类在运行时写入。
     */
    public static class SmokeListener {

        final AtomicReference<MQEvent> received = new AtomicReference<>();

        /**
         * 记录消费管线投递到达的事件。
         *
         * @param event 投递到达的消息事件
         */
        @MQEventListener(topic = "", tags = TAG, group = "")
        public void onSmoke(MQEvent event) {
            received.set(event);
        }
    }
}
