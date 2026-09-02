package io.ddd4j.javalin.auth;

import com.google.inject.AbstractModule;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.core.util.SubjectKit;
import lombok.extern.slf4j.Slf4j;

/**
 * ddd4j-javalin-auth 各认证整合模块（license / satoken / security / shiro）的
 * 共享 Guice Module 骨架（模板方法模式）。
 *
 * <p>统一 {@code configure()} 流程，抽取四个模块真正的重复逻辑：
 * <ol>
 *   <li><b>子模块自有装配</b>：{@link #configureModule()} 钩子——绑定模块 Properties
 *       （如 license 的 {@code LicenseProperties}）或 broker 特定组件
 *       （如 sa-token 的注解处理器）</li>
 *   <li><b>SubjectProvider 注册</b>：子类通过 {@link #subjectProvider()} 提供实例
 *       （返回 {@code null} 表示本模块不注册，如 license），基类负责
 *       {@code bind(SubjectProvider.class)} eager 单例绑定 + 写回 {@link SubjectKit}
 *       静态注册中心（对标 Spring 的 {@code SubjectRegistrar} BeanPostProcessor）</li>
 * </ol>
 *
 * <p>各认证框架的静态异常处理器（{@code registerExceptionHandler(Javalin)}）因异常类型
 * 与响应码各不相同，属 broker 特定逻辑，不下沉到基类。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
@Slf4j
public abstract class AbstractAuthJavalinModule extends AbstractModule {

    /**
     * 模板方法：固定装配顺序——先子模块自有装配，再共享 SubjectProvider 注册。
     * 声明为 {@code final}，防止子类绕过骨架导致 SubjectKit 注册行为漂移。
     */
    @Override
    protected final void configure() {
        // 1. 子模块自有装配（Properties 绑定、broker 特定组件等）
        configureModule();
        // 2. 共享 SubjectProvider 装配：eager 绑定 + SubjectKit 写回
        registerSubjectProvider();
    }

    /**
     * 子模块自有装配钩子：绑定模块 Properties 或 broker 特定组件。
     * 默认空实现，按需覆写。
     */
    protected void configureModule() {
    }

    /**
     * SubjectProvider 工厂钩子：返回本模块要注册的 {@link SubjectProvider} 实例；
     * 返回 {@code null} 表示本模块不注册（如 license 模块仅提供 LicenseVerify）。
     */
    protected SubjectProvider subjectProvider() {
        return null;
    }

    /**
     * 共享装配：eager 绑定 SubjectProvider（单例）并写回 SubjectKit 静态注册中心，
     * 保证 {@code SubjectKit.getSubject()/login()/isLogin()} 等全局可用，
     * 无需业务方手动注册。
     */
    private void registerSubjectProvider() {
        SubjectProvider provider = subjectProvider();
        if (provider == null) {
            return;
        }
        // 创建 SubjectProvider 实例并 eager 绑定（单例）
        bind(SubjectProvider.class).toInstance(provider);
        // 【关键】对标 Spring SubjectRegistrar（BeanPostProcessor）：
        // Injector 创建即把 SubjectProvider 写回 SubjectKit 静态注册中心。
        SubjectKit.register(provider);
        log.info("{} registered to SubjectKit (eager, at Injector creation)",
                provider.getClass().getSimpleName());
    }
}
