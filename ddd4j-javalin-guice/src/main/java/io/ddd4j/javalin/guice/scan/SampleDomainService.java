package io.ddd4j.javalin.guice.scan;

import io.ddd4j.guice.annotation.ddd.DomainService;

/**
 * 业务示例：领域服务。
 * <p>标注了 ddd4j-javalin 的 {@code @DomainService}，应被 {@link DddAnnotationModule}
 * 通过 ClassGraph 扫描发现并自动绑定到 Guice。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@DomainService
public class SampleDomainService {

    public String hello() {
        return "hello from DDD service";
    }
}
