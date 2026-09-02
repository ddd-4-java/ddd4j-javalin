package io.ddd4j.core.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 具备失败回滚和关闭清理能力的全局 SPI 注册作用域。
 */
public final class SpiRegistrationScope implements AutoCloseable {

    /**
     * 1.0.x 改挂补钉：BaseContext（1.0.x）仅提供 inject/get/contains，无 remove；
     * 卸载时注入本哨兵占位，Contexts.get 读取到哨兵时按"未注册"处理。
     */
    static final Object REMOVED = new Object();

    private final List<Registration<?>> registrations = new ArrayList<>();
    private final List<Registration<?>> installed = new ArrayList<>();
    private boolean started;

    public synchronized <T> SpiRegistrationScope register(String key, Class<T> type, T service) {
        if (started) {
            throw new IllegalStateException("SPI scope has already started");
        }
        registrations.add(new Registration<>(key, type, service));
        return this;
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        try {
            for (Registration<?> registration : registrations) {
                registration.install();
                installed.add(registration);
            }
            started = true;
        } catch (RuntimeException exception) {
            rollback();
            throw exception;
        }
    }

    @Override
    public synchronized void close() {
        rollback();
        started = false;
        ThreadContext.clear();
    }

    private void rollback() {
        for (int index = installed.size() - 1; index >= 0; index--) {
            installed.get(index).uninstall();
        }
        installed.clear();
    }

    private static final class Registration<T> {

        private final String key;
        private final Class<T> type;
        private final T service;
        private Optional<T> previous = Optional.empty();

        private Registration(String key, Class<T> type, T service) {
            this.key = Objects.requireNonNull(key, "key must not be null");
            this.type = Objects.requireNonNull(type, "type must not be null");
            this.service = Objects.requireNonNull(service, "service must not be null");
        }

        private void install() {
            Object existing = BaseContext.get(key);
            previous = SpiRegistrationScope.REMOVED == existing || Objects.isNull(existing)
                    ? Optional.empty()
                    : Optional.of(type.cast(existing));
            BaseContext.inject(key, service);
        }

        private void uninstall() {
            Object current = BaseContext.get(key);
            if (current == service) {
                if (previous.isPresent()) {
                    BaseContext.inject(key, previous.orElseThrow());
                } else {
                    BaseContext.inject(key, SpiRegistrationScope.REMOVED);
                }
            }
            previous = Optional.empty();
        }
    }
}
