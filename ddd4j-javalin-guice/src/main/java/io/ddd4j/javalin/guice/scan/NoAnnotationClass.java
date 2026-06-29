package io.ddd4j.javalin.guice.scan;

/**
 * 无 DDD 注解的普通类：验证 DddAnnotationModule 不会绑定它。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public class NoAnnotationClass {
    public String value() {
        return "should-not-be-bound";
    }
}
