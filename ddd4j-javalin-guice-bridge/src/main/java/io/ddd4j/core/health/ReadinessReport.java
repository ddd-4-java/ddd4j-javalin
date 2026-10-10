package io.ddd4j.core.health;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 多个 {@link ReadinessContributor} 的聚合结果。
 *
 * <p>任一关键依赖未就绪或检查异常时，报告均为未就绪。检查异常只转换为安全的状态原因，
 * 原始异常应由 Runtime 的日志或观测系统记录。
 *
 * 组件 ready：是否可接收流量
 * 组件 results：每个已执行 Contributor 的结果
 */
public final class ReadinessReport {

    private static final long serialVersionUID = 0L;

    private final boolean ready;

    private final List<ReadinessResult> results;

    @JsonCreator()
    public ReadinessReport(@JsonProperty("ready") boolean ready, @JsonProperty("results") List<ReadinessResult> results) {
        results = List.copyOf(Objects.requireNonNullElse(results, List.of()));
        this.ready = ready;
        this.results = results;
    }

    /**
     * 执行并汇总贡献者。
     *
     * @param contributors 依赖检查器集合
     * @return 聚合就绪报告
     */
    public static ReadinessReport check(Collection<? extends ReadinessContributor> contributors) {
        List<ReadinessResult> results = Objects.requireNonNullElse(contributors, List.<ReadinessContributor>of()).stream().filter(Objects::nonNull).map(ReadinessReport::checkContributor).toList();
        return new ReadinessReport(results.stream().allMatch(ReadinessResult::ready), results);
    }

    private static ReadinessResult checkContributor(ReadinessContributor contributor) {
        try {
            ReadinessResult result = contributor.check();
            return Objects.requireNonNullElseGet(result, () -> ReadinessResult.unavailable(contributor.getClass().getSimpleName(), "empty result"));
        } catch (RuntimeException exception) {
            return ReadinessResult.unavailable(contributor.getClass().getSimpleName(), "check failed");
        }
    }

    @JsonProperty("ready")
    public boolean ready() {
        return ready;
    }

    @JsonProperty("results")
    public List<ReadinessResult> results() {
        return results;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        ReadinessReport other = (ReadinessReport) obj;
        return this.ready == other.ready && Objects.equals(this.results, other.results);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Boolean.hashCode(ready);
        result = 31 * result + Objects.hashCode(results);
        return result;
    }

    @Override
    public String toString() {
        return "ReadinessReport[ready=" + ready + ", results=" + results + "]";
    }
}
