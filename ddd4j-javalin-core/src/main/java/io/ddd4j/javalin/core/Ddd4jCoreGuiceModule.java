package io.ddd4j.javalin.core;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.ddd4j.core.ddd.event.DomainEventPublisher;
import io.ddd4j.core.i18n.I18nProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.guice.Ddd4jGuiceModule;
import io.ddd4j.guice.event.GuiceDomainEventPublisher;
import io.ddd4j.guice.i18n.GuiceI18nProvider;
import io.ddd4j.guice.subject.GuiceSubjectProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * ddd4j-javalin 核心层聚合装配 Guice Module（对标 ddd4j-boot-core 的 {@code Ddd4jCoreAutoConfiguration}）。
 *
 * <p>{@code configure()} 中安装一个核心 Module 组合：
 * <ol>
 *   <li>{@link Ddd4jGuiceModule}（ddd4j-runtime-guice 提供的完整核心模块）：
 *       <ul>
 *         <li>3 个核心 SPI 绑定：{@link DomainEventPublisher} → {@link GuiceDomainEventPublisher}、
 *             {@link SubjectProvider} → {@link GuiceSubjectProvider}、{@link I18nProvider} → {@link GuiceI18nProvider}</li>
 *         <li>Guava {@code EventBus} 与 CQRS 读侧（ViewManager / ProjectionService / ProjectionRunner）</li>
 *         <li>{@code Ddd4jGuiceRuntime} eager singleton —— 启动即注册 SPI：
 *             {@code GuiceContext.setInjector(Injector)} + 向 ddd4j 全局上下文注册
 *             DOMAIN_EVENT_PUBLISHER / SUBJECT_PROVIDER / I18N_PROVIDER / COMMAND_BUS</li>
 *       </ul>
 *   </li>
 *   <li>显式声明核心 SPI 组合绑定（与 {@link Ddd4jGuiceModule} 一致；Guice 后绑定覆盖规则下
 *       保证装配自文档化，业务侧后续模块可覆盖）</li>
 * </ol>
 *
 * <p>开启/关闭语义：
 * <ul>
 *   <li>{@code ddd4j.core.enabled=false} —— 整体 back-off：不安装任何绑定，也不触发 SPI 注册</li>
 *   <li>{@code ddd4j.core.domain-events-enabled=false} —— 跳过本装配对
 *       {@code DomainEventPublisher} 的显式绑定声明（{@link Ddd4jGuiceModule} 的基础绑定仍然生效）</li>
 * </ul>
 *
 * <p>使用方式：
 * <pre>{@code
 * Injector injector = Guice.createInjector(Ddd4jCoreGuiceModule.defaults());
 * DomainEventPublisher publisher = injector.getInstance(DomainEventPublisher.class);
 * }</pre>
 */
@Slf4j
public class Ddd4jCoreGuiceModule extends AbstractModule {

    private final Ddd4jCoreProperties properties;

    public Ddd4jCoreGuiceModule() {
        this(new Ddd4jCoreProperties());
    }

    public Ddd4jCoreGuiceModule(Ddd4jCoreProperties properties) {
        this.properties = properties;
    }

    /**
     * 默认装配入口：全部开关开启（enabled / domainEventsEnabled / projectionEnabled 均为 true）。
     *
     * @return 默认核心装配模块
     */
    public static Ddd4jCoreGuiceModule defaults() {
        return new Ddd4jCoreGuiceModule();
    }

    @Override
    protected void configure() {
        if (!properties.isEnabled()) {
            log.info("ddd4j.core.enabled=false; ddd4j core layer (SPI bindings + registration) will not be installed");
            return;
        }
        bind(Ddd4jCoreProperties.class).toInstance(properties);
        // 核心 Module 组合（1）：ddd4j-runtime-guice 完整核心模块
        install(new Ddd4jGuiceModule());
        // 核心 Module 组合（2）：显式声明核心 SPI 绑定
        bind(SubjectProvider.class).to(GuiceSubjectProvider.class).in(Singleton.class);
        bind(I18nProvider.class).to(GuiceI18nProvider.class).in(Singleton.class);
        if (properties.isDomainEventsEnabled()) {
            bind(DomainEventPublisher.class).to(GuiceDomainEventPublisher.class).in(Singleton.class);
        } else {
            log.warn("ddd4j.core.domain-events-enabled=false; explicit DomainEventPublisher binding skipped "
                    + "(the Ddd4jGuiceModule base binding remains active)");
        }
    }

    /** 当前装配配置。可见于启动入口与测试。 */
    public Ddd4jCoreProperties getProperties() {
        return properties;
    }
}
