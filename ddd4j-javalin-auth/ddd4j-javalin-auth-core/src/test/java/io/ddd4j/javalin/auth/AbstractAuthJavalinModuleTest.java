package io.ddd4j.javalin.auth;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.subject.SubjectKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link AbstractAuthJavalinModule} 模板骨架契约测试。
 *
 * <p>验证两条路径：
 * <ol>
 *   <li>提供 {@code subjectProvider()} → eager 单例绑定 + SubjectKit 写回同一实例</li>
 *   <li>不提供（返回 null）→ 不创建 SubjectProvider 绑定，{@code configureModule()}
 *       钩子正常执行</li>
 * </ol>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class AbstractAuthJavalinModuleTest {

    /**
     * 最小 SubjectProvider 桩：接口方法均为 default，无需实现。
     */
    static class StubSubjectProvider implements SubjectProvider {
    }

    /**
     * 提供 subjectProvider() 时：SubjectProvider 可注入且为单例，
     * SubjectKit 静态注册中心写回的是同一实例。
     */
    @Test
    void shouldBindAndRegisterSubjectProvider() {
        Injector injector = Guice.createInjector(new AbstractAuthJavalinModule() {
            @Override
            protected SubjectProvider subjectProvider() {
                return new StubSubjectProvider();
            }
        });

        SubjectProvider bound = injector.getInstance(SubjectProvider.class);
        assertInstanceOf(StubSubjectProvider.class, bound);
        assertSame(bound, injector.getInstance(SubjectProvider.class), "SubjectProvider 应是单例");
        assertSame(bound, SubjectKit.subjectProvider, "SubjectKit 应写回同一 provider 实例");
    }

    /**
     * subjectProvider() 返回 null（默认）时：不创建 SubjectProvider 绑定，
     * configureModule() 钩子中的自有绑定正常生效。
     */
    @Test
    void shouldSkipSubjectProviderBindingWhenAbsent() {
        Injector injector = Guice.createInjector(new AbstractAuthJavalinModule() {
            @Override
            protected void configureModule() {
                bind(String.class).toInstance("custom");
            }
        });

        assertNull(injector.getExistingBinding(Key.get(SubjectProvider.class)),
                "未提供 subjectProvider() 时不应创建 SubjectProvider 绑定");
        assertEquals("custom", injector.getInstance(String.class),
                "configureModule() 钩子应正常执行");
    }
}
