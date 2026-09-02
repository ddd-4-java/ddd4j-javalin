package io.ddd4j.web.core.context;

import java.util.UUID;

/**
 * 为缺失请求标识的请求生成服务端标识。
 */
@FunctionalInterface
public interface RequestIdGenerator {

    String generate();

    static RequestIdGenerator uuid() {
        return () -> UUID.randomUUID().toString();
    }
}
