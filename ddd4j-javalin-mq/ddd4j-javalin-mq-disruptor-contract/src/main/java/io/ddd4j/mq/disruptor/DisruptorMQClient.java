package io.ddd4j.mq.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.util.TagMatcher;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * LMAX Disruptor（进程内 MQ）客户端实现（纯 Java，零 Spring 依赖）。
 *
 * <p>使用 ddd4j 自己的 {@link DisruptorEvent} 作为 RingBuffer 事件类型，
 * 以确保本地消息能力可由公开仓库独立构建。
 *
 * <h3>路由模型</h3>
 * <pre>
 *   namespace.topic.tag
 *   └───┬───┘ └─┬─┘ └┬┘
 *    环境隔离  业务分类  细分标签
 * </pre>
 *
 * <p>Disruptor 是进程内 RingBuffer，不存在可跨进程传输的 protocol header；稳定消息 ID 由
 * {@link MQEvent#getMsgId()} 同时保留在 payload 与 {@link DisruptorEvent} 字段中，不伪造
 * {@code ddd4j-message-id} metadata。
 *
 * <p>主线只有 {@link #initProducer(MQProperties)} 与 {@link #initConsumer(MQListener, MQProperties)}，
 * 核心业务逻辑全部内联。消费者模型：单 {@link com.lmax.disruptor.EventHandler} 内部遍历所有已注册 listener
 * （按 tag 过滤分发），对齐 base-mq RocketClient 的「集中消费者 + 应用层 tag 过滤」风格。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j(topic = "### DDD4J-MQ : DisruptorMQClient ###")
public class DisruptorMQClient implements MQClient {

    private final DisruptorMQProperties properties;

    /**
     * 已注册监听器列表（{@link #initConsumer} 时累加，{@link #onEvent} 在 RingBuffer 回调里遍历）。
     */
    private final List<MQListener> listeners = new CopyOnWriteArrayList<>();

    private Disruptor<DisruptorEvent> disruptor;

    @Getter
    private RingBuffer<DisruptorEvent> ringBuffer;

    /**
     * 双构造 1：传入配置，自建 RingBuffer 与 Disruptor（构造即启动，consumer 立即可用）。
     */
    public DisruptorMQClient(DisruptorMQProperties properties) {
        this.properties = Objects.requireNonNull(properties, "DisruptorMQ properties is required");
        this.startRingBuffer();
    }

    /**
     * 双构造 2：传入已初始化的 RingBuffer（runtime 集成复用同一 RingBuffer，事件类型必须为 DisruptorEvent）。
     * 不启动 Disruptor —— RingBuffer 已就绪，调用方负责管理其生命周期。
     */
    public DisruptorMQClient(RingBuffer<DisruptorEvent> ringBuffer) {
        this.properties = new DisruptorMQProperties();
        this.ringBuffer = Objects.requireNonNull(ringBuffer, "DisruptorMQ ringBuffer is required");
    }

    private static int normalizeBufferSize(int requested) {
        int size = 1;
        while (size < requested && size < (1 << 20)) {
            size <<= 1;
        }
        return size;
    }

    // ========================= 生产者 =========================

    @Override
    public String impl() {
        return "disruptor";
    }

    // ========================= 消费者 =========================

    @Override
    public Consumer<MQEvent> initProducer(MQProperties mqProperties) {
        return event -> {
            String topic = resolveTopic(event, mqProperties);
            String payload = serialization().serialize(event);
            long sequence = ringBuffer.next();
            try {
                DisruptorEvent e = ringBuffer.get(sequence);
                e.setTopic(topic);
                e.setTag(event.getTag());             // private 字段走 setter
                e.setNamespace(event.getNamespace());
                e.setMessageId(event.getMsgId());
                e.setPayload(payload);                // private 字段走 setter
                e.setSequence(sequence);
            } finally {
                ringBuffer.publish(sequence);
            }
            log.info("Publish MQ [{}]: {}", topic, payload);
        };
    }

    @Override
    public boolean initConsumer(MQListener listener, MQProperties mqProperties) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        log.info("Registered Disruptor consumer: bean={}, method={}",
                listener.getMethod().getDeclaringClass().getSimpleName(),
                listener.getMethod().getName());
        return true;
    }

    /**
     * RingBuffer 事件回调入口（{@link #start()} 注册到 Disruptor）。
     *
     * <p>匹配流程（统一遵循 {@code namespace.topic[.tag]} 路由模型）：
     * <ol>
     *   <li>route key 精确匹配：event 的 routeExpression vs listener 的 routeExpression</li>
     *   <li>{@link TagMatcher} 二次过滤（应对 listener.tags 表达式如 {@code "paid || shipped"}）</li>
     *   <li>反序列化 + 反射调用（{@link MQClient#consume} 默认方法）</li>
     * </ol>
     * 不匹配的 listener 跳过，避免跨 namespace/topic/tag 的误派发。
     */
    private void onEvent(DisruptorEvent event, long sequence, boolean endOfBatch) {
        String eventRouteKey = event.getRouteExpression();   // namespace.topic[.tag]
        String eventTag = event.getTag();
        for (MQListener listener : listeners) {
            try {
                // 1. route key 精确匹配（统一规则：event.getRouteExpression() == resolveRouteKey(listener)）
                if (!eventRouteKey.equals(resolveRouteKey(listener))) {
                    continue;
                }
                // 2. TagMatcher 二次过滤（应对 listener.tags 表达式）
                if (!TagMatcher.match(eventTag, listener.getTags())) {
                    continue;
                }
                // 3. 反序列化 + 反射调用
                Object payload = event.getPayload();
                MQEvent mqEvent = serialization().deserialize(payload, listener.payloadType());
                if (Objects.isNull(mqEvent)) {
                    log.warn("Disruptor consume [{}] failed: mqEvent is null", eventRouteKey);
                    continue;
                }
                DisruptorAcknowledgment ack = new DisruptorAcknowledgment(event, ringBuffer, sequence);
                consume(listener, mqEvent, ack);
                if (!ack.isAcknowledged()) {
                    ack.ackSingle();
                }
            } catch (Throwable ex) {
                log.error("Consume MQ [{}] failed", eventRouteKey, ex);
            }
        }
    }

    @Override
    public void start() {
        // Disruptor 在双构造 1 立即启动（双构造 2 RingBuffer 已就绪），
        // 此处留空对齐 MQClient.start() 契约。
    }

    /**
     * 启动 RingBuffer 与 Disruptor（双构造 1 自动调用，双构造 2 不调用）。
     */
    private void startRingBuffer() {
        int bufferSize = normalizeBufferSize(properties.getBufferSize());
        WaitStrategy waitStrategy = properties.getWaitStrategy().instance();
        Disruptor<DisruptorEvent> d = new Disruptor<>(DisruptorEvent::new, bufferSize,
                DaemonThreadFactory.INSTANCE, ProducerType.MULTI, waitStrategy);
        d.handleEventsWith(this::onEvent);
        ringBuffer = d.start();
        this.disruptor = d;
        log.info("Disruptor RingBuffer started: bufferSize={}, waitStrategy={}",
                bufferSize, waitStrategy.getClass().getSimpleName());
    }

    /**
     * 关闭底层 Disruptor（应用关闭或测试清理时调用）。
     */
    @Override
    public void close() {
        if (Objects.nonNull(disruptor)) {
            disruptor.shutdown();
            log.info("DisruptorMQClient shutdown");
        }
    }
}
