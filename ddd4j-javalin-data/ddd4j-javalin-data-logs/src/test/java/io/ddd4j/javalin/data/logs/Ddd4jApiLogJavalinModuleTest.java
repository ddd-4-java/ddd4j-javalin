package io.ddd4j.javalin.data.logs;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.data.logs.ApiOperationLogProvider;
import io.ddd4j.data.logs.DefaultApiOperationLogProvider;
import io.ddd4j.data.logs.aspect.ApiOperationLogAspect;
import org.junit.jupiter.api.Test;
import org.aspectj.lang.ProceedingJoinPoint;
import io.swagger.v3.oas.annotations.Operation;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies Guice assembly of {@link Ddd4jApiLogJavalinModule}: the
 * {@code ApiOperationLogProvider} is bound to its default implementation and the
 * aspect is resolvable.
 */
class Ddd4jApiLogJavalinModuleTest {

    @Test
    void shouldBindDefaultProviderAndAspect() {
        Injector injector = Guice.createInjector(new Ddd4jApiLogJavalinModule());

        ApiOperationLogProvider provider = injector.getInstance(ApiOperationLogProvider.class);
        ApiOperationLogAspect aspect = injector.getInstance(ApiOperationLogAspect.class);

        assertThat(provider).isNotNull().isInstanceOf(DefaultApiOperationLogProvider.class);
        assertThat(aspect).isNotNull();
    }

    @Test
    void shouldRespectUserOverriddenProvider() {
        ApiOperationLogProvider custom = new ApiOperationLogProvider() {
        };
        Injector injector = Guice.createInjector(
                new Ddd4jApiLogJavalinModule() {
                    @Override
                    protected void configure() {
                        bind(ApiOperationLogProvider.class).toInstance(custom);
                    }
                });
        assertThat(injector.getInstance(ApiOperationLogProvider.class)).isSameAs(custom);
        assertThat(injector.getInstance(ApiOperationLogAspect.class)).isNotNull();
    }

    @Test
    void shouldExecuteCompleteSuccessAndFailureLogLifecycle() throws Throwable {
        AtomicInteger before = new AtomicInteger();
        AtomicInteger returned = new AtomicInteger();
        AtomicInteger throwing = new AtomicInteger();
        ApiOperationLogProvider provider = new ApiOperationLogProvider() {
            @Override public void doBefore(org.aspectj.lang.JoinPoint point, Operation operation) { before.incrementAndGet(); }
            @Override public void afterReturing(org.aspectj.lang.JoinPoint point, Operation operation,
                                                Object result, com.google.common.base.Stopwatch stopwatch) {
                returned.incrementAndGet();
            }
            @Override public void afterThrowing(org.aspectj.lang.JoinPoint point, Operation operation,
                                                Throwable error, com.google.common.base.Stopwatch stopwatch) {
                throwing.incrementAndGet();
            }
        };
        Injector injector = Guice.createInjector(new Ddd4jApiLogJavalinModule(provider));
        ApiOperationLogAspect aspect = injector.getInstance(ApiOperationLogAspect.class);
        Operation operation = operation();
        ProceedingJoinPoint success = mock(ProceedingJoinPoint.class);
        when(success.proceed()).thenReturn("ok");

        assertThat(aspect.aroundMethod(success, operation)).isEqualTo("ok");
        ProceedingJoinPoint failure = mock(ProceedingJoinPoint.class);
        when(failure.proceed()).thenThrow(new IllegalStateException("boom"));
        assertThatThrownBy(() -> aspect.aroundMethod(failure, operation))
                .isInstanceOf(IllegalStateException.class).hasMessage("boom");
        assertThat(before).hasValue(2);
        assertThat(returned).hasValue(1);
        assertThat(throwing).hasValue(1);
    }

    private Operation operation() throws Exception {
        Method method = LoggedContract.class.getDeclaredMethod("invoke");
        return method.getAnnotation(Operation.class);
    }

    static class LoggedContract {
        @Operation(summary = "test")
        void invoke() {
        }
    }
}
