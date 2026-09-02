package io.ddd4j.javalin.core;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.context.SpiRegistrationScope;
import io.ddd4j.core.context.Contexts;
import io.ddd4j.core.cqrs.command.CommandBus;
import io.ddd4j.core.cqrs.readmodel.InMemoryProjectionPositionRepository;
import io.ddd4j.core.cqrs.readmodel.ProjectionPositionRepository;
import io.ddd4j.core.ddd.event.DomainEventPublisher;
import io.ddd4j.core.i18n.I18nProvider;
import io.ddd4j.core.subject.SubjectProvider;
import io.ddd4j.guice.Ddd4jGuiceRuntime;
import io.ddd4j.guice.context.GuiceContext;
import io.ddd4j.guice.event.GuiceDomainEventPublisher;
import io.ddd4j.guice.i18n.GuiceI18nProvider;
import io.ddd4j.guice.subject.GuiceSubjectProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Ddd4jCoreGuiceModule} and {@link Ddd4jCoreAutoConfiguration}:
 * the unified core assembly entry for ddd4j-javalin (aligned with ddd4j-boot-core).
 */
class Ddd4jCoreGuiceModuleTest {

    private Injector injector;

    @AfterEach
    void tearDown() {
        // 恢复全局状态：关闭 Ddd4jGuiceRuntime（回滚 BaseContext 核心 SPI 注册 + GuiceContext.clear()）
        if (Objects.nonNull(injector)
                && Objects.nonNull(injector.getExistingBinding(Key.get(Ddd4jGuiceRuntime.class)))) {
            injector.getInstance(Ddd4jGuiceRuntime.class).close();
        }
        // 手工装配入口注册的投影 SPI 不在 Ddd4jGuiceRuntime 作用域内，显式移除。
        // 1.0.x 改挂：BaseContext 无 remove(...)，注入 SpiRegistrationScope.REMOVED 哨兵占位
        // （Contexts.get 读取到哨兵按"未注册"处理）。
        BaseContext.inject(SpiKeys.PROJECTION_POSITION_REPOSITORY, SpiRegistrationScope.REMOVED);
        GuiceContext.clear();
    }

    @Test
    void shouldResolveCoreSpiAfterInstall() {
        injector = Guice.createInjector(new Ddd4jCoreGuiceModule());

        // 3 个核心 SPI 的 Guice 实现可解析
        SubjectProvider subjectProvider = injector.getInstance(SubjectProvider.class);
        assertThat(subjectProvider).isInstanceOf(GuiceSubjectProvider.class);

        DomainEventPublisher publisher = injector.getInstance(DomainEventPublisher.class);
        assertThat(publisher).isInstanceOf(GuiceDomainEventPublisher.class);

        I18nProvider i18nProvider = injector.getInstance(I18nProvider.class);
        assertThat(i18nProvider).isInstanceOf(GuiceI18nProvider.class);

        // CQRS 命令总线可解析（Ddd4jGuiceModule @Provides 装配）
        assertThat(injector.getInstance(CommandBus.class)).isNotNull();

        // 启动即注册 SPI：Guice 容器已注册到 GuiceContext
        assertThat(GuiceContext.isInitialized()).isTrue();
        assertThat(GuiceContext.getInstance(SubjectProvider.class)).isSameAs(subjectProvider);

        // 4 个核心 SPI 已注册到 ddd4j 全局上下文（Ddd4jGuiceRuntime eager singleton 完成）
        assertThat(Contexts.get(SpiKeys.DOMAIN_EVENT_PUBLISHER, DomainEventPublisher.class)).isPresent();
        assertThat(Contexts.get(SpiKeys.SUBJECT_PROVIDER, SubjectProvider.class)).isPresent();
        assertThat(Contexts.get(SpiKeys.I18N_PROVIDER, I18nProvider.class)).isPresent();
        assertThat(Contexts.get(SpiKeys.COMMAND_BUS, CommandBus.class)).isPresent();
    }

    @Test
    void shouldInstallViaDefaultsFactory() {
        injector = Guice.createInjector(Ddd4jCoreGuiceModule.defaults());

        assertThat(injector.getInstance(SubjectProvider.class)).isNotNull();
        assertThat(injector.getInstance(DomainEventPublisher.class)).isNotNull();
        assertThat(GuiceContext.isInitialized()).isTrue();
    }

    @Test
    void shouldBackOffWhenDisabled() {
        Ddd4jCoreProperties properties = new Ddd4jCoreProperties();
        properties.setEnabled(false);
        injector = Guice.createInjector(new Ddd4jCoreGuiceModule(properties));

        // enabled=false：不绑定任何核心 SPI，也不触发运行时注册
        assertThat(injector.getExistingBinding(Key.get(SubjectProvider.class))).isNull();
        assertThat(injector.getExistingBinding(Key.get(DomainEventPublisher.class))).isNull();
        assertThat(injector.getExistingBinding(Key.get(Ddd4jGuiceRuntime.class))).isNull();
        assertThat(GuiceContext.isInitialized()).isFalse();
        assertThat(Contexts.get(SpiKeys.SUBJECT_PROVIDER, SubjectProvider.class)).isEmpty();
    }

    @Test
    void shouldRegisterProjectionSpiViaAutoConfigurationInstall() {
        // 手工/选择性装配场景：不安装 Ddd4jGuiceModule，仅提供投影 SPI 所需绑定
        injector = newManualInjector();

        Ddd4jCoreAutoConfiguration.install(injector);

        assertThat(GuiceContext.isInitialized()).isTrue();
        ProjectionPositionRepository repository = injector.getInstance(ProjectionPositionRepository.class);
        assertThat(Contexts.get(SpiKeys.PROJECTION_POSITION_REPOSITORY, ProjectionPositionRepository.class))
                .hasValueSatisfying(registered -> assertThat(registered).isSameAs(repository));
    }

    @Test
    void shouldSkipProjectionSpiWhenDisabled() {
        injector = newManualInjector();

        Ddd4jCoreProperties properties = new Ddd4jCoreProperties();
        properties.setProjectionEnabled(false);
        Ddd4jCoreAutoConfiguration.install(injector, properties);

        // 容器仍注册到 GuiceContext，但投影 SPI 不注册
        assertThat(GuiceContext.isInitialized()).isTrue();
        assertThat(Contexts.get(SpiKeys.PROJECTION_POSITION_REPOSITORY, ProjectionPositionRepository.class))
                .isEmpty();
    }

    /**
     * 构造仅含手工装配所需绑定的 Injector（EventBus + DomainEventPublisher + 投影位置仓储）。
     */
    private static Injector newManualInjector() {
        return Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(DomainEventPublisher.class).to(GuiceDomainEventPublisher.class).in(Singleton.class);
                bind(ProjectionPositionRepository.class)
                        .to(InMemoryProjectionPositionRepository.class).in(Singleton.class);
            }

            @Provides
            @Singleton
            EventBus eventBus() {
                return new EventBus();
            }
        });
    }
}
