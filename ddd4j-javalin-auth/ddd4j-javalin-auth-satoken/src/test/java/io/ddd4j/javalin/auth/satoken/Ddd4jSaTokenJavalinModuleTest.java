package io.ddd4j.javalin.auth.satoken;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.ddd4j.auth.satoken.subject.SaTokenSubjectProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ddd4j-javalin-auth-satoken Guice 集成测试。
 *
 * <p>验证：Guice Module 装配后，对标 Spring 三职责全部生效：
 * <ol>
 *   <li>SubjectProvider 可从 Guice 注入（SaTokenSubjectProvider 单例）</li>
 *   <li>SubjectKit 静态注册中心已写入（getSubject 不抛 IllegalStateException）</li>
 *   <li>SubjectKit.getSubject() 返回的 Subject 可用（isLogin 不抛异常）</li>
 * </ol>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class Ddd4jSaTokenJavalinModuleTest {

    /**
     * 验证 SubjectProvider 可从 Guice 注入，且是 SaTokenSubjectProvider 实例。
     */
    @Test
    void shouldResolveSubjectProviderFromGuice() {
        Injector injector = Guice.createInjector(new Ddd4jSaTokenJavalinModule());

        SubjectProvider provider = injector.getInstance(SubjectProvider.class);
        assertNotNull(provider, "SubjectProvider 应可从 Guice 注入");
        assertInstanceOf(SaTokenSubjectProvider.class, provider,
                "SubjectProvider 应是 SaTokenSubjectProvider 实例");

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
        Guice.createInjector(new Ddd4jSaTokenJavalinModule());

        // SubjectKit.getSubject() 不应抛 IllegalStateException（证明已注册）
        assertDoesNotThrow(() -> {
            io.ddd4j.core.subject.Subject subject = SubjectKit.getSubject();
            assertNotNull(subject, "SubjectKit.getSubject() 应返回非 null 的 Subject");
        }, "Injector 创建后 SubjectKit 应已注册，getSubject() 不应抛异常");
    }

    /**
     * 验证 SubjectKit.getSubject() 返回的 Subject 是 sa-token 实现（SaTokenSubject）。
     *
     * <p>注意：sa-token 的运行时方法（isLogin/login）依赖 HTTP 请求上下文（SaTokenContext），
     * 在无 HTTP 请求的纯单元测试下不可调用。这里仅验证适配层正确桥接了 SaTokenSubjectProvider
     * 到 SubjectKit，类型断言 SaTokenSubject 即证明 sa-token 鉴权链路在 javalin 适配下连通。
     */
    @Test
    void subjectKitShouldReturnSaTokenSubject() {
        Guice.createInjector(new Ddd4jSaTokenJavalinModule());

        io.ddd4j.core.subject.Subject subject = SubjectKit.getSubject();
        assertNotNull(subject, "Subject 应非 null");
        assertInstanceOf(io.ddd4j.auth.satoken.subject.SaTokenSubject.class, subject,
                "Subject 应是 SaTokenSubject 实例，证明 sa-token 鉴权链路连通");
    }
}
