package io.ddd4j.core.api;

import io.ddd4j.core.exception.BizRuntimeException;

import java.io.Serializable;
import java.util.Objects;

/**
 * 统一接口响应，标准的响应数据结构
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public interface IR extends Serializable {
    Serializable getCode();

    String getMsg();

    <T> T getData();

    Boolean isOk();

    default void isOk(String notOkThrows) {
        if (!isOk()) {
            throw new BizRuntimeException(notOkThrows + " -> " + this);
        }
    }

    default <T> T getData(String notOkThrows) {
        if (!isOk()) {
            throw new BizRuntimeException(notOkThrows + " -> " + this);
        }
        T data = getData();
        if (Objects.isNull(data)) {
            throw new BizRuntimeException(notOkThrows + " -> " + this);
        }
        return data;
    }
}