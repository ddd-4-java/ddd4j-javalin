package io.ddd4j.mq.redisstream;

import io.ddd4j.kit.lang.StrKit;
import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.listener.MQListener;
import io.ddd4j.mq.util.TagMatcher;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.UnifiedJedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Redis Pubsub 客户端实现（纯 Java，零 Spring 依赖）。
 *
 * <p>100% 对齐 base-mq {@code RedisClient} 风格：仅 3 个公开方法
 * {@link #impl()} / {@link #initProducer} / {@link #initConsumer}，
 * 逻辑全部内联，守护线程循环。
 *
 * <p>因 ddd4j 无独立 redis 模块，故与 Redis Stream 共置 {@code ddd4j-mq-redis-stream} 模块，
 * 复用 {@link RedisStreamMQProperties} 获取连接配置。
 *
 * <ul>
 *   <li>{@link #initProducer} —— {@link BlockingQueue} + 守护线程，循环 take 后 {@code jedis.publish(channel, payload)}，
 *       {@link MQEvent#publish()} 通过它把消息推送到 broker</li>
 *   <li>{@link #initConsumer} —— 守护线程 {@code jedis.subscribe(JedisPubSub, channels)}，
 *       {@code onMessage} 回调里反序列化后调 {@link #consume(MQListener, MQEvent)}（pubsub 无 ack）</li>
 * </ul>
 *
 * <p>Channel 通过 {@link MQClient#resolveTopic(MQEvent, MQProperties)} /
 * {@link MQClient#resolveTopic(MQListener, MQProperties)} 解析；默认拼接符 {@code :}（Redis 命名习惯）。
 * Redis pubsub 无 broker 端 tag 过滤，走应用层 {@link TagMatcher#match}。它也没有可与 payload
 * 一起传递的 per-message metadata，因此稳定消息 ID 只保留在 {@link MQEvent} payload 中。
 *
 * <p>注：构造方法 1 接收 {@link UnifiedJedis} 而非旧版 {@code Jedis}——Jedis 7.x 已将
 * {@code Jedis} 与 {@code UnifiedJedis} 拆为兄弟接口，标准 Jedis 7.x 客户端（{@code JedisPooled} 等）均注入此类型。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Slf4j(topic = "### DDD4J-MQ : redisMQClient ###")
public class RedisMQClient implements MQClient {

    private final AtomicBoolean started = new AtomicBoolean(false);
    /**
     * 双构造：构造方法 1 直接持有外部注入的 Jedis 客户端（UnifiedJedis 形态）。
     */
    private final UnifiedJedis injectedJedis;
    /**
     * 双构造：构造方法 2 持有 properties，第一次调用时 lazy 构造 Jedis。
     */
    private final RedisStreamMQProperties properties;
    BlockingQueue<MQEvent> SENDING_MSGS = new LinkedBlockingQueue<>();
    /**
     * lazy 构造的 Jedis（volatile 保证发布可见性）。
     */
    private volatile UnifiedJedis lazyJedis;

    public RedisMQClient(UnifiedJedis jedis) {
        this.injectedJedis = jedis;
        this.properties = null;
    }

    public RedisMQClient(RedisStreamMQProperties properties) {
        this.injectedJedis = null;
        this.properties = properties;
    }

    /**
     * 获取当前实例使用的 Jedis 客户端（注入优先，否则 lazy 构造）。
     */
    private UnifiedJedis jedis() {
        if (Objects.nonNull(injectedJedis)) {
            return injectedJedis;
        }
        UnifiedJedis j = lazyJedis;
        if (Objects.isNull(j)) {
            synchronized (this) {
                j = lazyJedis;
                if (Objects.isNull(j)) {
                    j = properties.newJedis();
                    lazyJedis = j;
                }
            }
        }
        return j;
    }

    @Override
    public String impl() {
        return "redis";
    }

    /**
     * Redis Pubsub 默认拼接符 {@code :}（Redis 命名习惯）。
     */
    @Override
    public String defaultConcat() {
        return ":";
    }

    // ========================= 生产者 =========================

    @Override
    public Consumer<MQEvent> initProducer(MQProperties properties) {
        if (started.compareAndSet(false, true)) {
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ddd4j-redis-pubsub-publisher");
                t.setDaemon(true);
                return t;
            }).submit(() -> {
                log.info("MQ publisher start");
                while (!Thread.currentThread().isInterrupted()) {
                    String channel = null;
                    String payload = null;
                    try {
                        MQEvent mqEvent = SENDING_MSGS.take();
                        payload = serialization().serialize(mqEvent).toString();
                        channel = resolveTopic(mqEvent, properties);
                        jedis().publish(channel, payload);
                        log.info("Publish MQ [{}]: {}", channel, payload);
                    } catch (Exception e) {
                        log.error("Publish MQ [{}]: {} failed!", channel, payload, e);
                    }
                }
                log.info("MQ publisher stopped");
            });
        }
        return mqEvent -> SENDING_MSGS.offer(mqEvent);
    }

    // ========================= 消费者 =========================

    @Override
    public boolean initConsumer(MQListener listener, MQProperties properties) throws Exception {
        // channel=namespace:topic[:tag]；多正向 tag 各订阅一份
        List<String> channels = new ArrayList<>();
        String mainChannel = resolveTopic(listener, properties);
        channels.add(mainChannel);
        if (StrKit.isNotEmpty(listener.getTags())) {
            Set<String> tags = TagMatcher.findIncludes(listener.getTags());
            for (String tag : tags) {
                String c = resolveTopic(namespace(listener.getNamespace(), properties),
                        listener.getTopic(), tag, concat(null));
                if (!channels.contains(c)) {
                    channels.add(c);
                }
            }
        }
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ddd4j-redis-pubsub-" + listener.getRouteExpression(this.defaultConcat()));
            t.setDaemon(true);
            return t;
        }).submit(() -> {
            try {
                jedis().subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        MQEvent mqEvent;
                        try {
                            mqEvent = serialization().deserialize(message, listener.payloadType());
                        } catch (Exception ex) {
                            log.warn("Consume MQ [{}] failed: deserialize error", listener.getRouteExpression(RedisMQClient.this.defaultConcat()), ex);
                            return;
                        }
                        if (Objects.isNull(mqEvent)) {
                            log.warn("Consume MQ [{}] failed: the mqEvent is null", listener.getRouteExpression(RedisMQClient.this.defaultConcat()));
                            return;
                        }
                        if (!TagMatcher.match(mqEvent.getTag(), listener.getTags())) {
                            return;
                        }
                        try {
                            // pubsub 无 ack 概念
                            consume(listener, mqEvent);
                        } catch (Throwable e) {
                            log.error("Consume MQ [{}] failed: {}", listener.getRouteExpression(RedisMQClient.this.defaultConcat()), message, e);
                        }
                    }

                    @Override
                    public void onSubscribe(String channel, int subscribedChannels) {
                        log.info("Subscribed channel: {}", channel);
                    }

                    @Override
                    public void onUnsubscribe(String channel, int subscribedChannels) {
                        log.info("Unsubscribed channel: {}", channel);
                    }
                }, channels.toArray(new String[0]));
            } catch (Exception e) {
                log.error("Subscribe MQ [{}] failed!", listener.getRouteExpression(this.defaultConcat()), e);
            }
        });
        return true;
    }

}
