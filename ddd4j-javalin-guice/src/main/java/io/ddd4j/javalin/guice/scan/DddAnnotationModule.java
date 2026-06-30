package io.ddd4j.javalin.guice.scan;

/**
 * DDD 注解扫描绑定 Module：用 ClassGraph 扫描 DDD 注解标注的类，自动绑定到 Guice。
 *
 * <p><b>核心目标</b>：让 Javalin/Guice 像 Spring 那样"自动发现"DDD 注解标注的类并注册为 Bean。
 *
 * <p>Guice 是"显式绑定"容器，本身不会扫描 {@code @Singleton} 等元注解。本 Module 通过
 * ClassGraph 在启动时扫描指定包下标注 DDD 注解（{@code @DomainService} / {@code @DomainRepository}
 * / {@code @ApplicationService} 等）的具体类，调用 {@link #bind(Class)} 注册到 Guice。
 *
 * <h3>使用方式</h3>
 * <pre>
 * Injector injector = Guice.createInjector(
 *     new DddModule(),                                   // 基础设施绑定
 *     new DddAnnotationModule("com.example.app")         // DDD 注解自动扫描绑定
 * );
 * </pre>
 *
 * <h3>扫描的注解</h3>
 * <ul>
 *   <li>{@code io.ddd4j.guice.annotation.ddd.DomainService}</li>
 *   <li>{@code io.ddd4j.guice.annotation.ddd.DomainRepository}</li>
 *   <li>{@code io.ddd4j.guice.annotation.ddd.ApplicationService}</li>
 *   <li>{@code io.ddd4j.guice.annotation.ddd.QueryService}</li>
 *   <li>{@code io.ddd4j.guice.annotation.ddd.CommandExecutor}</li>
 *   <li>{@code io.ddd4j.guice.annotation.ddd.DomainEntity}</li>
 *   <li>{@code io.ddd4j.guice.annotation.ddd.DomainValueObject}</li>
 *   <li>{@code io.ddd4j.guice.annotation.ddd.DomainGateway}</li>
 *   <li>{@code io.ddd4j.guice.annotation.ddd.DomainAssembler}</li>
 *   <li>{@code io.ddd4j.guice.annotation.ddd.DomainConverter}</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Deprecated
public class DddAnnotationModule extends io.ddd4j.guice.scan.DddAnnotationModule {

    /**
     * 构造 DDD 注解扫描 Module。
     *
     * @param basePackages 要扫描的基础包（如业务项目的根包 {@code com.example.app}）
     */
    public DddAnnotationModule(String... basePackages) {
        super(basePackages);
    }

    /**
     * 构造 DDD 注解扫描 Module（可禁用扫描，仅用于测试）。
     *
     * @param enableClassGraph 是否启用 ClassGraph 扫描
     * @param basePackages     要扫描的基础包
     */
    public DddAnnotationModule(boolean enableClassGraph, String... basePackages) {
        super(enableClassGraph, basePackages);
    }
}
