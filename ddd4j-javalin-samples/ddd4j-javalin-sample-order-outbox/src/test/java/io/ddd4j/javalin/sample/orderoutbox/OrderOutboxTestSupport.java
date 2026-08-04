package io.ddd4j.javalin.sample.orderoutbox;

import io.ddd4j.cache.subject.InMemorySubject;
import io.ddd4j.cache.subject.InMemorySubjectProvider;
import io.ddd4j.core.auth.AuthRequest;
import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.Contexts;
import io.ddd4j.core.subject.SubjectProvider;

/**
 * 集成测试公共支撑：向 ddd4j 全局 SPI 注册内存 Subject，并提供可用于
 * {@code Authorization: Bearer <token>} 请求头的会话凭证。
 *
 * <p>ddd4j-javalin 统一请求生命周期默认 {@code AuthenticationMode.REQUIRED}
 * （非公开路径必须携带有效 Bearer Token），与 ddd4j-boot-sample-order 的
 * {@code OrderControllerTest} 使用同一套 InMemorySubject 登录流程。
 */
final class OrderOutboxTestSupport {

    /** 已登录会话的 Bearer 请求头值。 */
    static final String AUTHORIZATION;

    static {
        InMemorySubject subject = new InMemorySubject(event -> {
            // 示例仅演示 Subject SPI 桥接，不额外持久化认证事件。
        });
        Contexts.register(SpiKeys.SUBJECT_PROVIDER, SubjectProvider.class, new InMemorySubjectProvider(subject));
        AUTHORIZATION = "Bearer " + subject.login(AuthRequest.of("order-outbox-it-user"));
    }

    private OrderOutboxTestSupport() {
    }
}
