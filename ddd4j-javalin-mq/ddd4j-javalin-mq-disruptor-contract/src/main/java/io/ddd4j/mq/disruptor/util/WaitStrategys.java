package io.ddd4j.mq.disruptor.util;

import com.lmax.disruptor.*;

/**
 * LMAX Disruptor 等待策略枚举。
 *
 * <p>每个枚举值封装对应的 {@link WaitStrategy} 实例（按需 new，单例复用 OK），同时作为
 * {@link io.ddd4j.mq.disruptor.DisruptorMQProperties#setWaitStrategy} 字段类型，
 * Spring Boot 配置反序列化时按枚举名（{@code blocking} / {@code yielding} / {@code busyspin} / {@code sleeping}）匹配。
 *
 * <p>各策略特性：
 * <ul>
 *   <li>{@link #blocking} —— BlockingWaitStrategy，最低 CPU 消耗</li>
 *   <li>{@link #sleeping} —— SleepingWaitStrategy，对生产者线程影响最小</li>
 *   <li>{@link #yielding} —— YieldingWaitStrategy，低延迟（推荐）</li>
 *   <li>{@link #busy_spin} —— BusySpinWaitStrategy，性能最高但 CPU 消耗最大</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public enum WaitStrategys {

    /**
     * 最低 CPU 消耗，适合各种部署环境。
     */
    blocking {
        @Override
        public WaitStrategy instance() {
            return new BlockingWaitStrategy();
        }
    },
    /**
     * 对生产者线程影响最小，适合异步日志类场景。
     */
    sleeping {
        @Override
        public WaitStrategy instance() {
            return new SleepingWaitStrategy();
        }
    },
    /**
     * 低延迟、CPU 消耗中等（推荐默认）。
     */
    yielding {
        @Override
        public WaitStrategy instance() {
            return new YieldingWaitStrategy();
        }
    },
    /**
     * 性能最高，CPU 消耗最大，建议消费者线程数 < 物理核数时使用。
     */
    busy_spin {
        @Override
        public WaitStrategy instance() {
            return new BusySpinWaitStrategy();
        }
    };

    /**
     * 获取对应的 {@link WaitStrategy} 实例。
     */
    public abstract WaitStrategy instance();
}
