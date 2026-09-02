package io.ddd4j.web.core.context;

import io.ddd4j.core.context.ThreadContext;

import java.util.Map;
import java.util.Objects;

/**
 * 请求级 ThreadContext 作用域（1.0.x 改挂补钉）。
 *
 * <p>2.0.x 的 {@code ThreadContext.open()}/{@code ThreadContext.Scope} 在 1.0.x core 中不存在
 * （1.0.x ThreadContext 仅保留 get/set/clear 静态工具）。本类以 getValues()/setValues() 快照语义
 * 等价还原"进入时快照、关闭时恢复"的作用域行为，供 WebContextScope 使用。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 6.7.x
 */
final class ThreadContextScope implements AutoCloseable {

    private final Map<String, Object> previousValues;
    private boolean closed;

    private ThreadContextScope(Map<String, Object> previousValues) {
        this.previousValues = previousValues;
    }

    /**
     * 打开作用域：快照当前线程上下文。
     */
    static ThreadContextScope open() {
        return new ThreadContextScope(ThreadContext.getValues());
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        ThreadContext.clear();
        if (Objects.nonNull(previousValues)) {
            ThreadContext.setValues(previousValues);
        }
        closed = true;
    }
}
