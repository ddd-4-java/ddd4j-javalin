package io.ddd4j.javalin.data.mybatis;

import com.google.inject.Injector;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** MyBatis repository initialization and resource cleanup contracts. */
class MybatisRepositoryLifecycleParticipantTest {

    @Test
    void shouldInitializeAndCloseExactlyOnce() {
        AtomicInteger initializations = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();
        SqlSession session = proxy(SqlSession.class, (proxy, method, arguments) -> {
            if ("close".equals(method.getName())) {
                closes.incrementAndGet();
            }
            return null;
        });
        Injector injector = proxy(Injector.class, (proxy, method, arguments) -> {
            if ("getInstance".equals(method.getName()) && arguments[0] == SqlSession.class) {
                return session;
            }
            return null;
        });
        Ddd4jMybatisJavalinModule module = new Ddd4jMybatisJavalinModule(null) {
            @Override
            public void initRepositories(Injector ignored) {
                initializations.incrementAndGet();
            }
        };
        MybatisRepositoryLifecycleParticipant participant =
                new MybatisRepositoryLifecycleParticipant(injector, module);

        participant.start();
        participant.start();
        participant.close();
        participant.close();

        assertThat(initializations).hasValue(1);
        assertThat(closes).hasValue(1);
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{type}, handler);
    }
}
