package io.ddd4j.javalin.core;

import com.google.inject.Injector;
import io.ddd4j.core.constant.SpiKeys;
import io.ddd4j.core.context.BaseContext;
import io.ddd4j.core.cqrs.readmodel.ProjectionPositionRepository;
import io.ddd4j.guice.context.GuiceContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * ddd4j-javalin 核心层静态装配入口（对标 ddd4j-boot-core 的
 * {@code Ddd4jCoreAutoConfiguration}「启动即注册 SPI」语义）。
 *
 * <p>与 boot 侧由 Spring 容器驱动 {@code @Import(SpringContextBridge)} 不同，Guice 侧将
 * 「启动即注册 SPI」收敛为静态入口 {@link #install(Injector)}：
 * <ol>
 *   <li>{@link GuiceContext#setInjector(Injector)} —— 把 Guice 容器注册到
 *       {@link GuiceContext}（对标 {@code SpringContextBridge} 的容器注册）</li>
 *   <li>按 {@link Ddd4jCoreProperties} 开关向 ddd4j 全局上下文注册 CQRS 读侧投影 SPI
 *       （{@code SpiKeys.PROJECTION_POSITION_REPOSITORY}，upstream {@code Ddd4jGuiceRuntime}
 *       未覆盖的 SPI）</li>
 * </ol>
 *
 * <p>注意：安装 {@link Ddd4jCoreGuiceModule}（内部 install {@code Ddd4jGuiceModule}）时，
 * {@code Ddd4jGuiceRuntime} eager singleton 已自动完成 GuiceContext 注册与 4 个核心 SPI
 * （DomainEventPublisher / SubjectProvider / I18nProvider / CommandBus）注册；本类用于
 * 「手工/选择性装配」场景，或按开关补充投影 SPI 注册。
 */
@Slf4j
public final class Ddd4jCoreAutoConfiguration {

    private Ddd4jCoreAutoConfiguration() {
    }

    /**
     * 默认装配：{@link Ddd4jCoreProperties} 全部开关开启。
     *
     * @param injector 已创建的 Guice 注入器
     */
    public static void install(Injector injector) {
        install(injector, new Ddd4jCoreProperties());
    }

    /**
     * 装配入口：注册 Guice 容器并按配置开关注册投影 SPI 到 ddd4j 全局上下文。
     *
     * @param injector   已创建的 Guice 注入器（不能为 {@code null}）
     * @param properties 核心层配置（不能为 {@code null}）
     */
    public static void install(Injector injector, Ddd4jCoreProperties properties) {
        Objects.requireNonNull(injector, "injector must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        if (!properties.isEnabled()) {
            log.info("ddd4j.core.enabled=false; ddd4j core SPI registration skipped");
            return;
        }
        if (!GuiceContext.isInitialized()) {
            GuiceContext.setInjector(injector);
        }
        if (properties.isProjectionEnabled()) {
            ProjectionPositionRepository repository =
                    injector.getInstance(ProjectionPositionRepository.class);
            BaseContext.inject(SpiKeys.PROJECTION_POSITION_REPOSITORY,
                    ProjectionPositionRepository.class, repository);
            log.info("ddd4j Core: registered projection position repository SPI: {}",
                    repository.getClass().getSimpleName());
        } else {
            log.info("ddd4j.core.projection-enabled=false; projection position repository SPI skipped");
        }
    }
}
