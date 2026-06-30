package io.ddd4j.javalin.guice.scan;

import com.google.inject.AbstractModule;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

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
public class DddAnnotationModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(DddAnnotationModule.class);

    /**
     * DDD 注解的全限定名列表（由 ddd4j-guice 提供）
     */
    private static final String[] DDD_ANNOTATION_NAMES = {
            "io.ddd4j.guice.annotation.ddd.DomainService",
            "io.ddd4j.guice.annotation.ddd.DomainRepository",
            "io.ddd4j.guice.annotation.ddd.ApplicationService",
            "io.ddd4j.guice.annotation.ddd.QueryService",
            "io.ddd4j.guice.annotation.ddd.CommandExecutor",
            "io.ddd4j.guice.annotation.ddd.DomainEntity",
            "io.ddd4j.guice.annotation.ddd.DomainValueObject",
            "io.ddd4j.guice.annotation.ddd.DomainGateway",
            "io.ddd4j.guice.annotation.ddd.DomainAssembler",
            "io.ddd4j.guice.annotation.ddd.DomainConverter"
    };

    private final String[] basePackages;
    private final boolean enableClassGraph;

    /**
     * 构造 DDD 注解扫描 Module。
     *
     * @param basePackages 要扫描的基础包（如业务项目的根包 {@code com.example.app}）
     */
    public DddAnnotationModule(String... basePackages) {
        this(true, basePackages);
    }

    /**
     * 构造 DDD 注解扫描 Module（可禁用扫描，仅用于测试）。
     *
     * @param enableClassGraph 是否启用 ClassGraph 扫描
     * @param basePackages     要扫描的基础包
     */
    public DddAnnotationModule(boolean enableClassGraph, String... basePackages) {
        this.enableClassGraph = enableClassGraph;
        this.basePackages = basePackages == null ? new String[0] : basePackages;
    }

    @Override
    protected void configure() {
        if (!enableClassGraph) {
            logger.info("DddAnnotationModule: 扫描已禁用（enableClassGraph=false）");
            return;
        }
        scanAndBind();
    }

    /**
     * 用 ClassGraph 扫描 DDD 注解标注的类并绑定到 Guice。
     */
    private void scanAndBind() {
        long start = System.currentTimeMillis();
        Set<Class<?>> boundClasses = new LinkedHashSet<>();

        try (ScanResult scan = new ClassGraph()
                .acceptPackages(basePackages)
                .enableClassInfo()
                .enableAnnotationInfo()
                .ignoreMethodVisibility()
                .scan()) {

            for (String annotationName : DDD_ANNOTATION_NAMES) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends Annotation> annotationType =
                            (Class<? extends Annotation>) Class.forName(annotationName);

                    for (ClassInfo ci : scan.getClassesWithAnnotation(annotationName)) {
                        if (!ci.isStandardClass()) {
                            continue;  // 跳过接口/注解/抽象类
                        }
                        try {
                            Class<?> clazz = ci.loadClass();
                            if (boundClasses.add(clazz)) {
                                // 判断是否应该绑定为单例：
                                // 1. 类上直接标注了 @Singleton
                                // 2. 或类上标注的 DDD 注解（如 @DomainService）元标注了 @Singleton
                                boolean singleton = isSingletonScoped(clazz, annotationType);
                                if (singleton) {
                                    bind(clazz).in(com.google.inject.Scopes.SINGLETON);
                                } else {
                                    bind(clazz);
                                }
                                logger.debug("DDD 绑定: {} (@{}) scope={}", clazz.getName(),
                                        annotationType.getSimpleName(),
                                        singleton ? "SINGLETON" : "DEFAULT");
                            }
                        } catch (IllegalArgumentException e) {
                            logger.warn("DDD 绑定失败（可能是抽象类/接口）: {}", ci.getName(), e);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    logger.debug("DDD 注解类不在 classpath: {}", annotationName);
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        logger.info("DddAnnotationModule 扫描完成: 扫描包={}, 绑定 {} 个 DDD Bean (耗时 {}ms)",
                Arrays.toString(basePackages), boundClasses.size(), elapsed);
    }

    /**
     * 判断类是否应该绑定为单例。
     *
     * <p>检查两层：
     * <ol>
     *   <li>类上直接标注了 Guice {@code @Singleton}</li>
     *   <li>类上标注的 DDD 注解（如 {@code @DomainService}）元标注了 {@code @Singleton}</li>
     * </ol>
     */
    private boolean isSingletonScoped(Class<?> clazz, Class<? extends Annotation> dddAnnotationType) {
        // 1. 类上直接标注了 @Singleton
        if (clazz.isAnnotationPresent(com.google.inject.Singleton.class)) {
            return true;
        }
        // 2. DDD 注解本身元标注了 @Singleton（递归检查元注解链）
        if (dddAnnotationType != null
                && dddAnnotationType.isAnnotationPresent(com.google.inject.Singleton.class)) {
            return true;
        }
        return false;
    }
}
