package io.ddd4j.javalin.api.cqrs;

import io.ddd4j.core.cqrs.projection.ProjectionPosition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Javalin 内存版投影位置（POJO，无 JPA 依赖）。
 *
 * <p>Javalin 是轻量级框架，业务方可通过 JDBI / JDBC 自行持久化，
 * 本类仅作为内存示例。生产环境建议替换为 JDBI {@code @RegisterBeanMapper}。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JavalinJpaProjectionPosition implements ProjectionPosition, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String streamId;
    private long nextEventNumber;

    @Override
    public ProjectionPosition withNextEventNumber(long nextEventNumber) {
        this.nextEventNumber = nextEventNumber;
        return this;
    }
}
