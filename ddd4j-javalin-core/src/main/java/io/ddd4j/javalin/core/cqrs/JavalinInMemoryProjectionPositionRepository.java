package io.ddd4j.javalin.core.cqrs;

import io.ddd4j.guice.cqrs.GuiceInMemoryProjectionPositionRepository;

/**
 * Javalin 内存版投影位置仓储（最小化实现）。
 *
 * <p>Javalin 是轻量级框架，业务方可通过 JDBI / JDBC / 其他持久化方案替换。
 * 本类仅作为开箱即用的内存版（重启后位置丢失，仅适合开发/测试）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Deprecated
public class JavalinInMemoryProjectionPositionRepository extends GuiceInMemoryProjectionPositionRepository {

    @Override
    public void resetToZero(String streamId) {
        super.resetToZero(streamId);
    }
}
