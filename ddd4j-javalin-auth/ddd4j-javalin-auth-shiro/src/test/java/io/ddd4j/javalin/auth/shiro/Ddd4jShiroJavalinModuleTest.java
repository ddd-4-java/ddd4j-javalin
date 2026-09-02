package io.ddd4j.javalin.auth.shiro;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.auth.shiro.subject.ShiroSubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.subject.SubjectKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ddd4j-javalin-auth-shiro Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，对标 Spring 三职责全部生效：
 * <ol>
 *   <li>SubjectProvider 可从 Guice 注入（ShiroSubjectProvider 单例）</li>
 *   <li>SubjectKit 静态注册中心已写入（getSubject 不抛 IllegalStateException）</li>
 *   <li>SubjectKit.getSubject() 返回的 Subject 是 ShiroSubject（鉴权链路连通）</li>
 * </ol>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jShiroJavalinModuleTest {

    /**
     * 验证 SubjectProvider 可从 Guice 注入，且是 ShiroSubjectProvider 实例。
     */
    @Test
    void shouldResolveSubjectProviderFromGuice() {
        Injector injector = Guice.createInjector(new Ddd4jShiroJavalinModule());

        SubjectProvider provider = injector.getInstance(SubjectProvider.class);
        assertNotNull(provider, "SubjectProvider 应可从 Guice 注入");
        assertInstanceOf(ShiroSubjectProvider.class, provider,
                "SubjectProvider 应是 ShiroSubjectProvider 实例");

        // 单例：多次获取应是同一实例
        SubjectProvider provider2 = injector.getInstance(SubjectProvider.class);
        assertSame(provider, provider2, "SubjectProvider 应是单例");
    }

    /**
     * 验证 Injector 创建后 SubjectKit 静态注册中心已写入。
     *
     * <p>对标 Spring SubjectRegistrar（BeanPostProcessor）：
     * Injector 创建即触发 SubjectKit.register()，无需业务方手动注册。
     */
    @Test
    void shouldRegisterToSubjectKitAtInjectorCreation() {
        // 创建 Injector —— 此时应触发 SubjectKit.register(provider)
        Guice.createInjector(new Ddd4jShiroJavalinModule());

        // SubjectKit.getSubject() 不应抛 IllegalStateException（证明已注册）
        assertDoesNotThrow(() -> {
            io.ddd4j.core.subject.Subject subject = SubjectKit.getSubject();
            assertNotNull(subject, "SubjectKit.getSubject() 应返回非 null 的 Subject");
        }, "Injector 创建后 SubjectKit 应已注册，getSubject() 不应抛异常");
    }

    /**
     * 验证 SubjectKit.getSubject() 返回的 Subject 是 Shiro 实现（ShiroSubject）。
     *
     * <p>类型断言 ShiroSubject 即证明 Shiro 鉴权链路在 javalin 适配下连通可用。
     */
    @Test
    void subjectKitShouldReturnShiroSubject() {
        Guice.createInjector(new Ddd4jShiroJavalinModule());

        io.ddd4j.core.subject.Subject subject = SubjectKit.getSubject();
        assertNotNull(subject, "Subject 应非 null");
        assertInstanceOf(io.ddd4j.auth.shiro.subject.ShiroSubject.class, subject,
                "Subject 应是 ShiroSubject 实例，证明 Shiro 鉴权链路连通");
    }
}
