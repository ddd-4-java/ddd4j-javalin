package io.ddd4j.javalin.core.cqrs;

import io.ddd4j.core.cqrs.projection.ProjectionPosition;
import io.ddd4j.core.cqrs.projection.ProjectionPositionRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Javalin 内存版投影位置仓储（最小化实现）。
 *
 * <p>Javalin 是轻量级框架，业务方可通过 JDBI / JDBC / 其他持久化方案替换。
 * 本类仅作为开箱即用的内存版（重启后位置丢失，仅适合开发/测试）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public class JavalinInMemoryProjectionPositionRepository implements ProjectionPositionRepository {

    private final ConcurrentMap<String, ProjectionPosition> store = new ConcurrentHashMap<>();

    @Override
    public Optional<ProjectionPosition> findByStreamId(String streamId) {
        return Optional.ofNullable(store.get(streamId));
    }

    @Override
    public List<ProjectionPosition> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public ProjectionPosition save(ProjectionPosition position) {
        store.put(position.getStreamId(), position);
        return position;
    }

    @Override
    public void deleteByStreamId(String streamId) {
        store.remove(streamId);
    }

    @Override
    public void resetToZero(String streamId) {
        ProjectionPosition old = store.get(streamId);
        if (old != null) {
            store.put(streamId, old.withNextEventNumber(0L));
        }
    }

    /**
     * 当前仓储容量（仅用于监控 / 测试断言）。
     */
    public int size() {
        return store.size();
    }

    /**
     * 清空全部位置（测试用）。
     */
    public void clear() {
        store.clear();
    }

    /**
     * 导出全部位置（用于外部持久化）。
     */
    public Map<String, ProjectionPosition> snapshot() {
        return Map.copyOf(store);
    }
}
