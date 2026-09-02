package io.ddd4j.guice.cqrs;

import io.ddd4j.core.cqrs.readmodel.ProjectionPosition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Guice 环境默认的内存投影位置对象。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuiceProjectionPosition implements ProjectionPosition, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 事件流 ID
     */
    private String streamId;
    /**
     * 下一条待处理的事件序号
     */
    private long nextEventNumber;

    @Override
    public ProjectionPosition withNextEventNumber(long nextEventNumber) {
        this.nextEventNumber = nextEventNumber;
        return this;
    }
}
