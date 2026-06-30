package io.ddd4j.javalin.core.cqrs;

import io.ddd4j.core.cqrs.projection.ProjectionPosition;
import io.ddd4j.guice.cqrs.GuiceProjectionPosition;

/**
 * Javalin 内存版投影位置（POJO，无 JPA 依赖）。
 *
 * <p>Javalin 是轻量级框架，业务方可通过 JDBI / JDBC 自行持久化，
 * 本类仅作为内存示例。生产环境建议替换为 JDBI {@code @RegisterBeanMapper}。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Deprecated
public class JavalinProjectionPosition extends GuiceProjectionPosition {

    public JavalinProjectionPosition() {
        super();
    }

    public JavalinProjectionPosition(String streamId, long nextEventNumber) {
        super(streamId, nextEventNumber);
    }

    @Override
    public ProjectionPosition withNextEventNumber(long nextEventNumber) {
        super.withNextEventNumber(nextEventNumber);
        return this;
    }
}
